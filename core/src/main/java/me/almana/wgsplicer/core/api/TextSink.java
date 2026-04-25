package me.almana.wgsplicer.core.api;

import me.almana.wgsplicer.core.domain.ExportResultMessage;

public interface TextSink {

    void info(String msg);

    void failure(String msg);

    void success(ExportResultMessage msg);

    ProgressBar createProgressBar(String label);
}
