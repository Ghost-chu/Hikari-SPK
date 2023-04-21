package com.ghostchu.web.hikarispk.controller;

import com.ghostchu.web.hikarispk.exception.ResourceNotFoundException;
import com.ghostchu.web.hikarispk.packages.SynoPackage;
import com.ghostchu.web.hikarispk.service.PackageService;
import com.google.common.collect.BiMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Controller
@RequestMapping("/package")
public class PackageController {
    @Autowired
    private PackageService packageService;
    @Autowired
    private HttpServletRequest request;

    @Value("${hikari-spk.packages.allow-direct-downloads}")
    private boolean allowDirectDownloads;

    @RequestMapping(value = "/thumbnail/{packageFileName}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] thumbnail(@PathVariable("packageFileName") String packageFileName) throws IOException {
        SynoPackage pkg = findPackage(packageFileName);
        if (pkg == null) {
            throw new ResourceNotFoundException();
        }
        return Files.readAllBytes(pkg.getPackageIcon256().toPath());
    }

    @RequestMapping(value = "/snapshot/{packageFileName}/{number}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] snapshot(@PathVariable("packageFileName") String packageFileName, @PathVariable("number") String number) throws IOException {
        if (!StringUtils.isNumeric(number)) {
            throw new IllegalArgumentException("Number must be a zero or a positive integer");
        }
        SynoPackage pkg = findPackage(packageFileName);
        if (pkg == null) {
            throw new ResourceNotFoundException();
        }
        if (pkg.getScreenshots().isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return Files.readAllBytes(pkg.getScreenshots().get(Math.min(pkg.getScreenshots().size() - 1, Integer.parseInt(number))).toPath());
    }

    @RequestMapping(value = "/download/{packageFileName}")
    @ResponseBody
    public void download(@PathVariable("packageFileName") String packageFileName, HttpServletResponse response) throws IOException {
        File pkgFile = findPackageFile(packageFileName);
        if (pkgFile == null) {
            throw new ResourceNotFoundException();
        }
        if (!allowDirectDownloads) {
            String userAgent = request.getHeader("User-Agent");
            if (!userAgent.contains("synology")) {
                response.sendError(403, "Direct packages downloading from Browser are not allowed");
                return;
            }
        }


        try (InputStream fis = new FileInputStream(pkgFile)) {
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(packageFileName, StandardCharsets.UTF_8));
            response.addHeader("Content-Length", String.valueOf(pkgFile.length()));
            response.setContentType("application/octet-stream");
            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            IOUtils.copy(fis, outputStream);
            outputStream.flush();
        }
    }

    @Nullable
    private SynoPackage findPackage(String packageFileName) {
        BiMap<File, SynoPackage> discoveredPackages = packageService.getDiscoveredPackages();
        for (Map.Entry<File, SynoPackage> entry : discoveredPackages.entrySet()) {
            if (entry.getKey().getName().equals(packageFileName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    private File findPackageFile(String packageFileName) {
        BiMap<File, SynoPackage> discoveredPackages = packageService.getDiscoveredPackages();
        for (Map.Entry<File, SynoPackage> entry : discoveredPackages.entrySet()) {
            if (entry.getKey().getName().equals(packageFileName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
