package com.recursive.infrastructure.system;

import com.recursive.domain.HardwareDetector;
import com.recursive.domain.HardwareSpec;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.GraphicsCard;
import oshi.software.os.OperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

/**
 * {@link HardwareDetector} implementation on OSHI. Reports measured facts
 * in gigabytes; the minimum-requirements verdict is the app's documented
 * default policy (8 GB RAM, 10 GB free disk).
 */
public class SystemMonitor implements HardwareDetector {

    private static final long MIN_RAM_GB = 8;
    private static final long MIN_FREE_DISK_GB = 10;

    private final SystemInfo systemInfo;

    public SystemMonitor() {
        this.systemInfo = new SystemInfo();
    }

    @Override
    public HardwareSpec detect() {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        long totalRamGb = toGb(hardware.getMemory().getTotal());
        long availableRamGb = toGb(hardware.getMemory().getAvailable());
        GraphicsCard gpu = hardware.getGraphicsCards().stream().findFirst().orElse(null);
        return new HardwareSpec(
                totalRamGb,
                availableRamGb,
                hardware.getProcessor().getLogicalProcessorCount(),
                gpu == null ? null : gpu.getName(),
                gpu == null ? 0 : gpu.getVRam(),
                freeDiskGb(),
                totalRamGb >= MIN_RAM_GB && freeDiskGb() >= MIN_FREE_DISK_GB);
    }

    private long freeDiskGb() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();
        long largest = 0;
        for (OSFileStore store : fileSystem.getFileStores()) {
            long usable = store.getUsableSpace();
            if (usable > largest) {
                largest = usable;
            }
        }
        return toGb(largest);
    }

    private static long toGb(long bytes) {
        return bytes / (1024L * 1024L * 1024L);
    }
}
