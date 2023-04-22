package com.ghostchu.web.hikarispk.service;

import com.ghostchu.web.hikarispk.config.HikariSPKConfig;
import com.ghostchu.web.hikarispk.packages.SynoPackage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PackageFilterService {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackageFilterService");
    private final Map<String, String> family2ArchMap = new HashMap<>();

    public PackageFilterService(@Autowired HikariSPKConfig hikariSPKConfig) throws ConfigurateException, FileNotFoundException {
        File modelsFile = new File(hikariSPKConfig.getDeviceListPath());
        if (!modelsFile.exists()) {
            throw new FileNotFoundException("Cannot found file: " + modelsFile.getAbsolutePath() + "!");
        }
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(modelsFile).build();
        CommentedConfigurationNode familyList = loader.load();
        LOGGER.info("Loading filter from file {}...", modelsFile.getAbsolutePath());
        for (CommentedConfigurationNode entry : familyList.childrenMap().values()) {
            String arch = String.valueOf(entry.key());
            for (CommentedConfigurationNode family : entry.childrenMap().values()) {
                family2ArchMap.put(String.valueOf(family.key()), arch);
            }
        }
        LOGGER.info("Found {} synology families.", family2ArchMap.size());
    }

    public List<SynoPackage> filter(List<SynoPackage> packages, @Nullable String archFilter, @Nullable String channelFilter, @Nullable String firmwareVersionFilter) throws SerializationException {
        List<SynoPackage> filtered = new ArrayList<>();
        for (SynoPackage pkg : packages) {
            if (filter(pkg, archFilter, channelFilter, firmwareVersionFilter)) {
                filtered.add(pkg);
            }
        }
        return filtered;
    }

    public boolean filter(SynoPackage pkg, @Nullable String archFilter, @Nullable String channelFilter, @Nullable String firmwareVersionFilter) throws SerializationException {
        if (archFilter != null) {
            if (!pkg.isCompatibleToArch(archFilter, family2ArchMap)) {
                return false;
            }
        }
        if (channelFilter != null) {
            if (channelFilter.equals("stable") && pkg.getBeta()) {
                return false;
            }
        }
        if (firmwareVersionFilter != null) {
            return pkg.isCompatibleToFirmware(firmwareVersionFilter);
        }
        return true;
    }

}
