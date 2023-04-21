package com.ghostchu.web.hikarispk.service;

import com.ghostchu.web.hikarispk.packages.SynoPackage;
import com.ghostchu.web.hikarispk.packages.SynoPackageParser;
import com.ghostchu.web.hikarispk.util.Glob;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.SneakyThrows;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PackageService {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Logger LOGGER = LoggerFactory.getLogger("PackageService");
    private final String packageFolderPath;
    private final String fileMask;
    private final String cachePath;
    private BiMap<File, SynoPackage> discoveredPackages = HashBiMap.create(new ConcurrentHashMap<>());


    public PackageService(@Value("${hikari-spk.paths.packages}") String packageFolderPath, @Value("${hikari-spk.packages.file-mask}") String fileMask, @Value("${hikari-spk.paths.cache}") String cachePath) throws Exception {
        this.packageFolderPath = packageFolderPath;
        this.cachePath = cachePath;
        this.fileMask = fileMask;
        loadPackages();
        FileAlterationObserver observer = new FileAlterationObserver(packageFolderPath);
        observer.addListener(new FileChangeListener(this, fileMask));
        FileAlterationMonitor monitor = new FileAlterationMonitor(10000, observer);
        monitor.start();
    }

    private void loadPackages() {
        LOGGER.info("Loading packages from disk...");
        long start = System.currentTimeMillis();
        File scanFolder = new File(packageFolderPath);
        if (!scanFolder.exists()) scanFolder.mkdirs();
        File[] files = scanFolder.listFiles((dir, name) -> name.matches(Glob.createRegexFromGlob(fileMask)));
        if (files == null) return;
        BiMap<File, SynoPackage> newPackages = HashBiMap.create(new ConcurrentHashMap<>());
        bakePackages(newPackages, files);
        this.discoveredPackages = newPackages;
        LOGGER.info("Loaded {} packages in {}ms.", this.discoveredPackages.size(), System.currentTimeMillis() - start);
    }

    private long bakePackages(Map<File, SynoPackage> packages, File[] files) {
        long start = System.currentTimeMillis();
        for (File file : files) {
            try {
                if (file.exists()) {
                    SynoPackage synoPackage = parsePackage(file);
                    if (synoPackage != null) {
                        packages.put(file, synoPackage);
                    }
                } else {
                    packages.remove(file);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse package file: " + file.getName() + " - " + e.getMessage(), e);
            }
        }
        return System.currentTimeMillis() - start;
    }


    public BiMap<File, SynoPackage> getDiscoveredPackages() {
        return HashBiMap.create(discoveredPackages);
    }

    public List<SynoPackage> getAllPackages() {
        return new ArrayList<>(discoveredPackages.values());
    }

    public SynoPackage parsePackage(File file) throws Exception {
        Future<SynoPackage> future = executorService.submit(() -> new SynoPackageParser(new File(cachePath), file).getSynoPackage());
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            LOGGER.warn("The package {} used too much time for parsing! Skipping...", file.getName());
            return null;
        }
    }

    public static class FileChangeListener extends FileAlterationListenerAdaptor {
        private final Logger LOGGER = LoggerFactory.getLogger("FileChangeListener");
        private final PackageService service;
        private final String fileMask;
        private final List<File> changedFiles = new ArrayList<>();
        private boolean anyChanges = false;

        public FileChangeListener(PackageService service, String fileMask) {
            this.service = service;
            this.fileMask = fileMask;
        }

        @Override
        public void onFileChange(File file) {
            if (file.getName().matches(Glob.createRegexFromGlob(fileMask))) {
                anyChanges = true;
                this.changedFiles.add(file);
                LOGGER.info("[#] {}", file.getName());
            }
        }

        @Override
        public void onFileCreate(File file) {
            if (file.getName().matches(Glob.createRegexFromGlob(fileMask))) {
                anyChanges = true;
                this.changedFiles.add(file);
                LOGGER.info("[+] {}", file.getName());
            }
        }

        @Override
        public void onFileDelete(File file) {
            if (file.getName().matches(Glob.createRegexFromGlob(fileMask))) {
                anyChanges = true;
                this.changedFiles.add(file);
                LOGGER.info("[-] {}", file.getName());
            }
        }

        @SneakyThrows
        @Override
        public void onStop(FileAlterationObserver observer) {
            if (anyChanges) {
                anyChanges = false;
                File[] changedFilesArray = this.changedFiles.toArray(new File[0]);
                this.changedFiles.clear();
                long cost = service.bakePackages(service.discoveredPackages, changedFilesArray);
                LOGGER.info("Reloaded packages information for {} package changes, total {} packages available, used {}ms.", changedFilesArray.length, service.discoveredPackages.size(), cost);

            }
        }
    }

}
