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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PackageService {
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
        observer.addListener(new FileChangeListener(this));
        FileAlterationMonitor monitor = new FileAlterationMonitor(5000, observer);
        monitor.start();
    }

    private void loadPackages() throws Exception {
        LOGGER.info("Loading packages from disk...");
        File scanFolder = new File(packageFolderPath);
        if (!scanFolder.exists()) scanFolder.mkdirs();
        File[] files = scanFolder.listFiles((dir, name) -> name.matches(Glob.createRegexFromGlob(fileMask)));
        if (files == null) return;
        BiMap<File, SynoPackage> packages = HashBiMap.create(new ConcurrentHashMap<>());
        for (File file : files) {
            try {
                packages.put(file, parsePackage(file));
            } catch (IOException e) {
                LOGGER.warn("Failed to parse package file: " + file.getName() + " - " + e.getMessage(), e);
            }
        }
        this.discoveredPackages = packages;
    }

    public BiMap<File, SynoPackage> getDiscoveredPackages() {
        return HashBiMap.create(discoveredPackages);
    }

    public List<SynoPackage> getAllPackages() {
        return new ArrayList<>(discoveredPackages.values());
    }

    public SynoPackage parsePackage(File file) throws Exception {
        return new SynoPackageParser(new File(cachePath), file).getSynoPackage();
    }

    public static class FileChangeListener extends FileAlterationListenerAdaptor {
        private final Logger LOGGER = LoggerFactory.getLogger("FileChangeListener");
        private final PackageService service;
        private boolean anyChanges = false;

        public FileChangeListener(PackageService service) {
            this.service = service;
        }

        @Override
        public void onFileChange(File file) {
            anyChanges = true;
            LOGGER.info("DetectedFile {} modified.", file.getName());
        }

        @Override
        public void onFileCreate(File file) {
            anyChanges = true;
            LOGGER.info("File {} created.", file.getName());
        }

        @Override
        public void onFileDelete(File file) {
            anyChanges = true;
            LOGGER.info("File {} deleted.", file.getName());
        }

        @SneakyThrows
        @Override
        public void onStop(FileAlterationObserver observer) {
            if (anyChanges) {
                anyChanges = false;
                LOGGER.info("Detected package file changes! Reloading packages information...");
                service.loadPackages();
            }
        }
    }

}
