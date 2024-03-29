package com.kylergib.wavelinktp.model;

/**
 * Mixers in wave link (local and stream)
 */
public class SwitchState {
    private String mixerId;
    private Boolean isMuted;
    private int level;
    private String mixerName;

    public SwitchState(String mixerId, Boolean isMuted, Integer level, String mixerName) {
        this.mixerId = mixerId;
        this.isMuted = isMuted;
        this.level = level;
        this.mixerName = mixerName;
    }

    public String getMixerId() {
        return mixerId;
    }

    public void setMixerId(String mixerId) {
        this.mixerId = mixerId;
    }

    public Boolean getMuted() {
        return isMuted;
    }

    public void setMuted(Boolean muted) {
        isMuted = muted;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
    public void setMixerName(String mixerName) {
        this.mixerName = mixerName;
    }
    public String getMixerName() {
        return mixerName;
    }
}
