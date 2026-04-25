package me.almana.wgsplicer.core.domain;

import java.nio.file.Path;

public record ExportResultMessage(int rows, Path json, String websiteUrl) {
}
