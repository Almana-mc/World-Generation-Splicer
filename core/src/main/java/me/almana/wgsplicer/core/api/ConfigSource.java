package me.almana.wgsplicer.core.api;

import java.util.List;

public interface ConfigSource {

    boolean scanEnabled();
    ScanMode scanMode();
    ScanTrigger scanTrigger();
    int maxScansPerTick();
    int maxInFlightWorkers();
    boolean rescanAlreadyScanned();
    int maxManualRadius();
    boolean includeBiomeStats();
    boolean includeYLevelStats();
    int yLevelBucketSize();
    List<String> extraBlockIds();
    List<String> extraNamespaces();
    List<String> dimensionBlacklist();

    boolean exportEnabled();
    String exportFolder();
    boolean exportAsync();
    String exportJsonFilenamePattern();
    boolean exportJsonPretty();
    String websiteUrl();

    int permissionLevel();
    boolean resetRequiresOp();
    boolean progressBarEnabled();
}
