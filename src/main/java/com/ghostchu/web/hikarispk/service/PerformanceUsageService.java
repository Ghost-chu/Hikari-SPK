package com.ghostchu.web.hikarispk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class PerformanceUsageService {
    private static final Logger LOGGER = LoggerFactory.getLogger("MemoryUsageService");

    public PerformanceUsageService() {
        Timer timer = new Timer();
        String interval = System.getProperty("hikarispk.memoryusage.interval", "5");
        String forcegc = System.getProperty("hikarispk.memoryusage.forcegc", "false");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
                long initMemorySize = memoryUsage.getInit();
                long maxMemorySize = memoryUsage.getMax();
                long usedMemorySize = memoryUsage.getUsed();
                LOGGER.info("Memory usage: {}MB used, {}MB max, {}MB free, {}MB init.",
                        usedMemorySize / (1024 * 1024),
                        maxMemorySize / (1024 * 1024),
                        (initMemorySize - usedMemorySize) / (1024 * 1024),
                        initMemorySize / (1024 * 1024)
                );
                if (Boolean.parseBoolean(forcegc)) {
                    System.gc();
                    MemoryMXBean memoryMXBeanAfter = ManagementFactory.getMemoryMXBean();
                    MemoryUsage memoryUsageAfter = memoryMXBeanAfter.getHeapMemoryUsage();
                    long initMemorySizeAfter = memoryUsageAfter.getInit();
                    long maxMemorySizeAfter = memoryUsageAfter.getMax();
                    long usedMemorySizeAfter = memoryUsageAfter.getUsed();
                    LOGGER.info("Memory usage (after GC): {}MB used, {}MB max, {}MB free, {}MB init.",
                            usedMemorySizeAfter / (1024 * 1024),
                            maxMemorySizeAfter / (1024 * 1024),
                            (initMemorySizeAfter - usedMemorySizeAfter) / (1024 * 1024),
                            initMemorySizeAfter / (1024 * 1024)
                    );
                }
            }
        }, 10 * 1000, Long.parseLong(interval) * 60 * 1000);
    }
}
