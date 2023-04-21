package com.ghostchu.web.hikarispk.packages;

import com.ghostchu.web.hikarispk.util.Glob;
import com.google.common.hash.Hashing;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SynoPackageParser {
    private static final Logger LOGGER = LoggerFactory.getLogger("SynoPackageParser");
    private final File synoPackageFile;
    private final SynoPackage synoPackage = new SynoPackage();
    private final File cacheFolder;

    public SynoPackageParser(File cacheFolder, File synoPackageFile) throws Exception {
        this.cacheFolder = cacheFolder;
        this.synoPackageFile = synoPackageFile;
        try (TarArchiveInputStream tarFile = new TarArchiveInputStream(new FileInputStream(synoPackageFile));) {
            parseFile(tarFile);
        }
    }


    private void parseInfo(String infoData) {

        Map<String, String> info = new LinkedHashMap<>();
        for (String s : infoData.split("\n")) {
            if (s.startsWith("#") || s.isBlank()) {
                continue;
            }
            String[] dat = s.split("=", 2);
            if (dat.length != 2) {
                continue;
            }
            dat[1] = dat[1].replaceAll("^\"|\"$", "");
            dat[1] = dat[1].replaceAll("\\\\", "");
            info.put(dat[0], dat[1]);
        }
        // validate
        if (info.get("package") == null) throw new IllegalArgumentException("package id not found in INFO file");
        if (info.get("version") == null) throw new IllegalArgumentException("package version not found in INFO file");
        if (info.get("os_min_ver") == null) throw new IllegalArgumentException("os_min_ver not found in INFO file");
        if (info.get("description") == null) throw new IllegalArgumentException("description not found in INFO file");
        if (info.get("arch") == null) throw new IllegalArgumentException("arch not found in INFO file");
        if (info.get("maintainer") == null) throw new IllegalArgumentException("maintainer not found in INFO file");
        // read
        synoPackage.setFileName(synoPackageFile.getName());
        synoPackage.setPackageId(info.get("package"));
        synoPackage.setVersion(info.get("version"));
        synoPackage.setOsMinVer(info.get("os_min_ver"));
        synoPackage.setDescription(info.get("description"));
        synoPackage.setArch(info.get("arch"));
        synoPackage.setMaintainer(info.get("maintainer"));
        synoPackage.setDisplayName(info.get("displayname"));
        synoPackage.setDistributor(info.get("distributor"));
        synoPackage.setDistributorUrl(info.get("distributor_url"));
        synoPackage.setSupportUrl(info.get("support_url"));
        synoPackage.setSupportCenter(parseBoolean(info.get("support_center")));
        synoPackage.setModel(info.get("model"));
        synoPackage.setExcludeArch(info.get("exclude_arch"));
        synoPackage.setChecksum(info.get("checksum"));
        synoPackage.setHelpUrl(info.get("helpurl"));
        synoPackage.setBeta(parseBoolean(info.get("beta")));
        synoPackage.setInstallDepPackages(info.get("install_dep_packages"));
        synoPackage.setInstallConflictPackages(info.get("install_conflict_packages"));
        synoPackage.setInstallBreakPackages(info.get("install_break_packages"));
        synoPackage.setInstallReplacePackages(info.get("install_replace_packages"));
        synoPackage.setInstallDepServices(info.get("install_dep_services"));
        if (info.get("extract_size") != null) synoPackage.setPackageSize(this.synoPackageFile.length());
        synoPackage.setInstallType(info.get("install_type"));
        synoPackage.setSilentInstall(parseBoolean(info.get("silent_install")));
        synoPackage.setSilentUpgrade(parseBoolean(info.get("silent_upgrade")));
        synoPackage.setSilentUninstall(parseBoolean(info.get("silent_uninstall")));
        synoPackage.setAutoUpgradeFrom(info.get("auto_upgrade_from"));
        synoPackage.setOsMaxVer(info.get("os_max_ver"));
        synoPackage.setExcludeModel(info.get("exclude_model"));
        synoPackage.setChangelog(info.get("changelog"));
        Map<String, String> localizedDisplayNames = new HashMap<>();
        info.entrySet().stream().filter(e -> e.getKey().startsWith("displayname_")).forEach(e -> localizedDisplayNames.put(e.getKey().substring(12), e.getValue()));
        synoPackage.setLocalizedDisplayName(localizedDisplayNames);
        Map<String, String> localizedDescriptions = new HashMap<>();
        info.entrySet().stream().filter(e -> e.getKey().startsWith("description_")).forEach(e -> localizedDescriptions.put(e.getKey().substring(12), e.getValue()));
        synoPackage.setLocalizedDescription(localizedDescriptions);

    }

    private void parseFile(TarArchiveInputStream tarArchive) throws IOException {
        File packageCacheDirectory = new File(cacheFolder, synoPackageFile.getName());
        if (packageCacheDirectory.exists()) {
            FileUtils.deleteDirectory(packageCacheDirectory);
        }
        TarArchiveEntry entry;
        boolean foundINFO = false;
        boolean foundIcon = false;
        boolean found256Icon = false;
        int loopTimes = 0;
        while ((entry = (TarArchiveEntry) tarArchive.getNextEntry()) != null) {
            loopTimes++;
            if (loopTimes > 3500 && foundINFO && foundIcon && found256Icon) {
                LOGGER.info("Too big package, skipping [{}] screenshot scanning! ", synoPackageFile.getName());
                break;
            }
            if (entry.isDirectory()) {
                if (!synoPackage.isHasWizardDir() && entry.getName().equals("WIZARD_UIFILES/")) {
                    synoPackage.setHasWizardDir(true);
                }
                continue;
            }
            if (!foundINFO && entry.getName().equals("INFO")) {
                foundINFO = true;
                parseInfo(new String(tarArchive.readAllBytes(), StandardCharsets.UTF_8));
            }
            if (entry.getName().matches(Glob.createRegexFromGlob("*_screen_*.png"))) {
                synoPackage.getScreenshots().add(saveCacheFile(tarArchive, packageCacheDirectory, "snapshots"));
            }
            if (!foundIcon && entry.getName().equals("PACKAGE_ICON.PNG")) {
                foundIcon = true;
                synoPackage.setPackageIcon(saveCacheFile(tarArchive, packageCacheDirectory, "package-icons"));
            }
            if (!found256Icon && entry.getName().equals("PACKAGE_ICON_256.PNG")) {
                found256Icon = true;
                synoPackage.setPackageIcon256(saveCacheFile(tarArchive, packageCacheDirectory, "package-icons"));
            }
        }
        if (!foundINFO) {
            throw new IOException("Bad package file, INFO file not found");
        }


    }

    private File saveCacheFile(TarArchiveInputStream tarArchive, File packageCacheDirectory, String category) throws IOException {
        File directory = new File(packageCacheDirectory, category);
        directory.mkdirs();
        byte[] picBytes = tarArchive.readAllBytes();
        String fileName = Hashing.sha256().hashBytes(picBytes) + ".png";
        File saveTo = new File(directory, fileName);
        Files.write(saveTo.toPath(), picBytes);
        return saveTo;
    }

    private boolean parseBoolean(String s) {
        return "yes".equals(s) || "true".equals(s) || "1".equals(s);
    }

    public SynoPackage getSynoPackage() {
        return synoPackage;
    }
}
