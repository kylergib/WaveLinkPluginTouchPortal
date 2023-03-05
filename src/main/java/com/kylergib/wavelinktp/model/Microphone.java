package com.kylergib.wavelinktp.model;
/**
 * Mic in wave link
 */
public class Microphone {


    private int id = 12;
    private String identifier;
    private Boolean isClipGuardOn;
    private Boolean isLowCutOn;
    private Boolean isWaveLink;
    private Boolean isWaveXLR;
    private int lowCutType;
    private String name;

    public Microphone(String identifier, Boolean isClipGuardOn, Boolean isLowCutOn, Boolean isWaveLink, Boolean isWaveXLR, int lowCutType, String name) {
        this.identifier = identifier;
        this.isClipGuardOn = isClipGuardOn;
        this.isLowCutOn = isLowCutOn;
        this.isWaveLink = isWaveLink;
        this.isWaveXLR = isWaveXLR;
        this.lowCutType = lowCutType;
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Boolean getClipGuardOn() {
        return isClipGuardOn;
    }

    public void setClipGuardOn(Boolean clipGuardOn) {
        isClipGuardOn = clipGuardOn;
    }

    public Boolean getLowCutOn() {
        return isLowCutOn;
    }

    public void setLowCutOn(Boolean lowCutOn) {
        isLowCutOn = lowCutOn;
    }

    public Boolean getWaveLink() {
        return isWaveLink;
    }

    public void setWaveLink(Boolean waveLink) {
        isWaveLink = waveLink;
    }

    public Boolean getWaveXLR() {
        return isWaveXLR;
    }

    public void setWaveXLR(Boolean waveXLR) {
        isWaveXLR = waveXLR;
    }

    public int getLowCutType() {
        return lowCutType;
    }

    public void setLowCutType(int lowCutType) {
        this.lowCutType = lowCutType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }


}
