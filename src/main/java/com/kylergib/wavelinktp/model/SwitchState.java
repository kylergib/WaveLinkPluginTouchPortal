package com.kylergib.wavelinktp.model;

public class SwitchState {
    private String mixerId;
    private Boolean isMuted;
    private int level;

    public SwitchState(String mixerId, Boolean isMuted, Integer level) {
        this.mixerId = mixerId;
        this.isMuted = isMuted;
        this.level = level;
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
}
