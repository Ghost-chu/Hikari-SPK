package com.ghostchu.web.hikarispk.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ghostchu.web.hikarispk.config.HikariSPKConfig;
import com.ghostchu.web.hikarispk.packages.SynoPackage;
import com.ghostchu.web.hikarispk.service.PackageFilterService;
import com.ghostchu.web.hikarispk.service.PackageService;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.serialize.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class SynoController {
    private static final Logger LOGGER = LoggerFactory.getLogger("SynoController");
    @Autowired
    private PackageService packageService;
    @Autowired
    private PackageFilterService packageFilterService;
    @Autowired
    private HikariSPKConfig hikariSPKConfig;


    @GetMapping("/spk")
    @ResponseBody
    public Object spkQuery(@RequestParam("language") String language, @NotBlank @RequestParam("timezone") String timezone, @NotBlank @RequestParam("unique") String unique, @NotBlank @RequestParam("arch") String arch, @NotBlank @RequestParam("major") int major, @NotBlank @RequestParam("minor") int minor, @NotBlank @RequestParam("build") int build, @NotBlank @RequestParam("package_update_channel") String packageUpdateChannel) throws SerializationException {
        language = language.trim();
        timezone = timezone.trim();
        unique = unique.trim();
        if (!unique.contains("synology")) {
            return Map.of("error", "Invalid Request");
        }
        arch = arch.trim();
        String firmwareVersion = major + "." + minor + "." + build;
        packageUpdateChannel = packageUpdateChannel.trim();
        List<SynoPackage> packageList = packageService.getAllPackages();
        packageList = packageFilterService.filter(packageList, arch, packageUpdateChannel, firmwareVersion);
        List<JsonOutput> outputs = new ArrayList<>();
        for (SynoPackage pkg : packageList) {
            outputs.add(new JsonOutput(hikariSPKConfig, pkg, language,
                    hikariSPKConfig.getPkgDescAppendHeader(), hikariSPKConfig.getPkgDescAppendFooter(),
                    hikariSPKConfig.getPkgChangelogAppendHeader(), hikariSPKConfig.getPkgChangelogAppendFooter()));
        }
        return outputs;
    }

    public static class JsonOutput {
        @JsonProperty("package")
        public String packageId;
        @JsonProperty("version")
        public String version;
        @JsonProperty("dname")
        public String displayName;
        @JsonProperty("desc")
        public String description;
        @JsonProperty("price")
        public Integer price;
        @JsonProperty("download_count")
        public Long downloadCount;
        @JsonProperty("recent_download_count")
        public Long recentDownloadCount;
        @JsonProperty("link")
        public String link;
        @JsonProperty("size")
        public Long size;
        @JsonProperty("thumbnail")
        public List<String> thumbnailUrl;
        @JsonProperty("snapshot")
        public List<String> snapshotUrls;
        @JsonProperty("qinst")
        public Boolean qInst;
        @JsonProperty("qstart")
        public Boolean qStart;
        @JsonProperty("qupgrade")
        public Boolean qUpgrade;
        @JsonProperty("depsers")
        public String depSers;
        @JsonProperty("deppkgs")
        public String depPkgs;
        @JsonProperty("conflictpkgs")
        public String conflictPkgs;
        @JsonProperty("start")
        public Boolean start;
        @JsonProperty("maintainer")
        public String maintainer;
        @JsonProperty("maintainer_url")
        public String maintainerUrl;
        @JsonProperty("distributor")
        public String distributor;
        @JsonProperty("distributor_url")
        public String distributorUrl;
        @JsonProperty("changelog")
        public String changeLog;
        @JsonProperty("support_url")
        public String supportUrl;
        @JsonProperty("thirdparty")
        public Boolean thirdParty;
        @JsonProperty("category")
        public Integer category;
        @JsonProperty("subcategory")
        public Integer subcategory;
        @JsonProperty("type")
        public Integer type;
        @JsonProperty("silent_install")
        public Boolean silentInstall;
        @JsonProperty("silent_uninstall")
        public Boolean silentUninstall;
        @JsonProperty("silent_upgrade")
        public Boolean silentUpgrade;
        @JsonProperty("auto_upgrade_from")
        public String autoUpgradeFrom;
        @JsonProperty("beta")
        public Boolean beta;

        public JsonOutput(HikariSPKConfig config, SynoPackage synoPackage, String language, String pkgDescAppendHeader, String pkgDescAppendFooter, String pkgChangelogAppendHeader, String pkgChangelogAppendFooter) {
            String baseUrl = config.getBaseUrl();
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.packageId = synoPackage.getPackageId();
            this.version = synoPackage.getVersion();
            this.displayName = synoPackage.getLocalizedDisplayName().getOrDefault(language, synoPackage.getDisplayName());
            this.description = synoPackage.getLocalizedDescription().getOrDefault(language, synoPackage.getDescription());
            if (StringUtils.isNotEmpty(pkgChangelogAppendHeader)) {
                this.description = pkgDescAppendHeader + this.description;
            }
            if (StringUtils.isNotEmpty(pkgChangelogAppendFooter)) {
                this.description = this.description + pkgDescAppendFooter;
            }
            this.price = 0;
            this.downloadCount = 0L;
            this.recentDownloadCount = 0L;
            this.link = baseUrl + "/package/download/" + synoPackage.getFileName();
            this.size = synoPackage.getExtractSize();
            this.thumbnailUrl = List.of(baseUrl + "/package/thumbnail/" + synoPackage.getFileName());
            if (!synoPackage.getScreenshots().isEmpty()) {
                List<String> snapshotUrls = new ArrayList<>();
                for (int i = 0; i <= synoPackage.getScreenshots().size(); i++) {
                    snapshotUrls.add(baseUrl + "/package/snapshot/" + synoPackage.getFileName() + "/" + i);
                }
                this.snapshotUrls = snapshotUrls;
            }
            this.qInst = !synoPackage.isHasWizardDir();
            this.qStart = !synoPackage.isHasWizardDir();
            this.qUpgrade = !synoPackage.isHasWizardDir();
            this.depSers = null;
            if (synoPackage.getInstallDepPackages() != null) {
                String depPkgs = synoPackage.getInstallDepPackages();
                for (String excludedSynoService : config.getExcludedSynoServices().split(" ")) {
                    depPkgs = depPkgs.replace(excludedSynoService, "");
                }
                depPkgs = depPkgs.trim();
                this.depPkgs = depPkgs;
            }
            if(StringUtils.isEmpty(this.depPkgs)) {
                this.depPkgs = null;
            }
            this.conflictPkgs = synoPackage.getInstallConflictPackages();
            this.start = true;
            this.maintainer = Objects.requireNonNullElse(synoPackage.getMaintainer(), config.getMaintainer());
            this.maintainerUrl = Objects.requireNonNullElse(synoPackage.getMaintainerUrl(), config.getMaintainerUrl());
            this.distributor = Objects.requireNonNullElse(synoPackage.getDistributor(), config.getDistributor());
            this.distributorUrl = Objects.requireNonNullElse(synoPackage.getDistributorUrl(), config.getDistributorUrl());
            this.changeLog = synoPackage.getChangelog();
            if (StringUtils.isNotEmpty(pkgChangelogAppendHeader)) {
                this.changeLog = pkgChangelogAppendHeader + this.changeLog;
            }
            if (StringUtils.isNotEmpty(pkgChangelogAppendFooter)) {
                this.changeLog = this.changeLog + pkgChangelogAppendFooter;
            }
            this.supportUrl = Objects.requireNonNullElse(synoPackage.getSupportUrl(), config.getSupportUrl());
            this.thirdParty = true;
            this.category = 0;
            this.subcategory = 0;
            this.type = 0;
            this.silentInstall = synoPackage.getSilentInstall();
            this.silentUninstall = synoPackage.getSilentUninstall();
            this.silentUpgrade = synoPackage.getSilentUpgrade();
            this.autoUpgradeFrom = synoPackage.getAutoUpgradeFrom();
            this.beta = synoPackage.getBeta();
        }
    }
}
