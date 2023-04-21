package com.ghostchu.web.hikarispk.packages;

import com.vdurmont.semver4j.Semver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SynoPackage {
    private static final Logger logger = LoggerFactory.getLogger(SynoPackage.class);
    private String fileName;
    private String packageId;
    private String version;
    private String osMinVer;
    private String description;
    private String arch;
    private String maintainer;
    private String displayName;
    private Map<String, String> localizedDisplayName = new HashMap<>();
    private Map<String, String> localizedDescription = new HashMap<>();
    private String maintainerUrl;
    private String distributor;
    private String distributorUrl;
    private String supportUrl;
    private Boolean supportCenter;
    private String model;
    private String excludeArch;
    private String checksum;
    private Boolean preCheckStartStop;
    private String helpUrl;
    private Boolean beta;
    private String installDepPackages;
    private String installConflictPackages;
    private String installBreakPackages;
    private String installReplacePackages;
    private String installDepServices;
    private Long extractSize;
    private Long packageSize;
    private String installType;
    private Boolean silentInstall;
    private Boolean silentUpgrade;
    private Boolean silentUninstall;
    private String autoUpgradeFrom;
    private String osMaxVer;
    private String excludeModel;
    private File packageIcon;
    private File packageIcon256;
    private boolean hasWizardDir;
    private List<File> screenshots = new ArrayList<>();
    private String changelog;


    public boolean isCompatibleToArch(@Nullable String deviceFamily, Map<String, String> family2ArchMap) {
        if (this.arch == null || this.arch.equals("noarch")) return true;
        String[] archs = this.arch.split(" ");
        String mappedArch = family2ArchMap.get(deviceFamily);
        for (String supportedArch : archs) {
            if (supportedArch.equals("noarch")) return true;
            if (supportedArch.equals(deviceFamily)) return true;
            if (mappedArch != null) {
                if (supportedArch.equals(mappedArch)) return true;
            }
        }
        return false;
    }

    public boolean isCompatibleToFirmware(@Nullable String firmware) {
        if (firmware == null) return true;
        if (this.osMinVer == null) return true;
        Semver packageSemver = new Semver(this.osMinVer, Semver.SemverType.LOOSE);
        Semver firmwareSemver = new Semver(firmware, Semver.SemverType.LOOSE);
        return packageSemver.isLowerThanOrEqualTo(firmwareSemver);


    }
}
