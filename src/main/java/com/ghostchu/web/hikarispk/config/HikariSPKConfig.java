package com.ghostchu.web.hikarispk.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class HikariSPKConfig {
    @Value("${hikari-spk.packages.allow-direct-downloads}")
    private boolean allowDirectDownloads;
    @Value("${hikari-spk.site.base-url}")
    private String baseUrl;
    @Value("${hikari-spk.excludedSynoServices}")
    private String excludedSynoServices;
    @Value("${hikari-spk.packages.maintainer}")
    private String maintainer;
    @Value("${hikari-spk.packages.maintainer-url}")
    private String maintainerUrl;
    @Value("${hikari-spk.packages.distributor}")
    private String distributor;
    @Value("${hikari-spk.packages.distributor-url}")
    private String distributorUrl;
    @Value("${hikari-spk.packages.support-url}")
    private String supportUrl;
    @Value("${hikari-spk.paths.models}")
    private String deviceListPath;
    @Value("${hikari-spk.paths.packages}")
    private String packageFolderPath;
    @Value("${hikari-spk.packages.file-mask}")
    private String fileMask;
    @Value("${hikari-spk.paths.cache}")
    private String cachePath;
    @Value("${hikari-spk.packages.description.append.header}")
    private String pkgDescAppendHeader;
    @Value("${hikari-spk.packages.description.append.footer}")
    private String pkgDescAppendFooter;
    @Value("${hikari-spk.packages.changelog.append.header}")
    private String pkgChangelogAppendHeader;
    @Value("${hikari-spk.packages.changelog.append.footer}")
    private String pkgChangelogAppendFooter;
}
