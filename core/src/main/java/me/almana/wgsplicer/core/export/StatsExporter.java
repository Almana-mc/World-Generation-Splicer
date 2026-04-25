package me.almana.wgsplicer.core.export;

import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.stats.StatsSnapshot;

import java.nio.file.Path;

public interface StatsExporter {

    record Result(Path file, int rows, boolean success, String message) {}

    Result export(StatsSnapshot snapshot, Path folder, String filename, ExportContext ctx) throws Exception;
}
