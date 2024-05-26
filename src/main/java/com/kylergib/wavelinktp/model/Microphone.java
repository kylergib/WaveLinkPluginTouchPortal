package com.kylergib.wavelinktp.model;

import java.math.BigDecimal;

/**
 * Mic in wave link
 */
public class Microphone {

    private String identifier;
    private Boolean isMicMuted;
    private BigDecimal outputVolume;
    private Boolean isClipGuardOn;
    private int lowCutType;
    private BigDecimal gain;
    private Boolean isWaveXLR;
    private Boolean isWaveLink;
    private BigDecimal balance;
    private Boolean isLowCutOn;
    private String name;
    private Boolean isGainLocked;


    public Microphone(String identifier, Boolean isMicMuted, BigDecimal outputVolume, Boolean isClipGuardOn, int lowCutType, BigDecimal gain, Boolean isWaveXLR, Boolean isWaveLink, BigDecimal balance, Boolean isLowCutOn, String name, Boolean isGainLocked) {
        this.identifier = identifier;
        this.isMicMuted = isMicMuted;
        this.outputVolume = outputVolume;
        this.isClipGuardOn = isClipGuardOn;
        this.lowCutType = lowCutType;
        this.gain = gain;
        this.isWaveXLR = isWaveXLR;
        this.isWaveLink = isWaveLink;
        this.balance = balance;
        this.isLowCutOn = isLowCutOn;
        this.name = name;
        this.isGainLocked = isGainLocked;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Boolean getMicMuted() {
        return isMicMuted;
    }

    public void setMicMuted(Boolean micMuted) {
        isMicMuted = micMuted;
    }

    public BigDecimal getOutputVolume() {
        return outputVolume;
    }

    public void setOutputVolume(BigDecimal outputVolume) {
        this.outputVolume = outputVolume;
    }

    public Boolean getClipGuardOn() {
        return isClipGuardOn;
    }

    public void setClipGuardOn(Boolean clipGuardOn) {
        isClipGuardOn = clipGuardOn;
    }

    public int getLowCutType() {
        return lowCutType;
    }

    public void setLowCutType(int lowCutType) {
        this.lowCutType = lowCutType;
    }

    public BigDecimal getGain() {
        return gain;
    }

    public void setGain(BigDecimal gain) {
        this.gain = gain;
    }

    public Boolean getWaveXLR() {
        return isWaveXLR;
    }

    public void setWaveXLR(Boolean waveXLR) {
        isWaveXLR = waveXLR;
    }

    public Boolean getWaveLink() {
        return isWaveLink;
    }

    public void setWaveLink(Boolean waveLink) {
        isWaveLink = waveLink;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Boolean getLowCutOn() {
        return isLowCutOn;
    }

    public void setLowCutOn(Boolean lowCutOn) {
        isLowCutOn = lowCutOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getGainLocked() {
        return isGainLocked;
    }

    public void setGainLocked(Boolean gainLocked) {
        isGainLocked = gainLocked;
    }
}
