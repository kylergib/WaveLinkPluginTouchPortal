package com.kylergib.wavelinktp.model;


/**
 * Config for all the outputs in wave link
 */
public class OutputConfig {
    private Boolean streamMixerMuted;
    private int streamMixerLevel;
    private Boolean localMixerMuted;
    private int localMixerLevel;

    public OutputConfig(Boolean streamMixerMuted, int streamMixerLevel,
                        Boolean localMixerMuted, int localMixerLevel) {
        this.streamMixerMuted = streamMixerMuted;
        this.streamMixerLevel = streamMixerLevel;
        this.localMixerMuted = localMixerMuted;
        this.localMixerLevel = localMixerLevel;
    }

    public Boolean getStreamMixerMuted() {
        return streamMixerMuted;
    }

    public void setStreamMixerMuted(Boolean streamMixerMuted) {
        this.streamMixerMuted = streamMixerMuted;
    }

    public int getStreamMixerLevel() {
        return streamMixerLevel;
    }

    public void setStreamMixerLevel(int streamMixerLevel) {
        this.streamMixerLevel = streamMixerLevel;
    }

    public Boolean getLocalMixerMuted() {
        return localMixerMuted;
    }

    public void setLocalMixerMuted(Boolean localMixerMuted) {
        this.localMixerMuted = localMixerMuted;
    }

    public int getLocalMixerLevel() {
        return localMixerLevel;
    }

    public void setLocalMixerLevel(int localMixerLevel) {
        this.localMixerLevel = localMixerLevel;
    }
}
