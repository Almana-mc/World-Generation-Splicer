package me.almana.wgsplicer.core.api;

public interface ProgressBar {

    void setProgress(float fraction, String title);

    void close();
}
