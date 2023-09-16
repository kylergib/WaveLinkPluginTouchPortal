package com.kylergib.wavelinktp.model;

import java.math.BigDecimal;
import java.util.ArrayList;
/**
 * Inputs in wave link
 */
public class Input {
    private int id = 16;
    private String identifier;
    private String name;
    private Boolean isAvailable;
    private Boolean streamMixerMuted;
    private Boolean localMixerMuted;
    private int localMixerLevel;
    private  int streamMixerLevel;
    private ArrayList<InputPlugin> plugins;
    private int inputType;
    private Boolean pluginBypassLocal; //if false then it is not bypassing the filter/plugin
    private Boolean pluginBypassStream; //if false then it is not bypassing the filter/plugin
    private BigDecimal levelLeft;
    private BigDecimal levelRight;

    //dynamic state info
//    private final String stateCategoryId = "WaveLinkInputs";
    private String localMuteStateId;
    private String streamMuteStateId;
    private String localFilterBypassStateId;
    private String streamFilterBypassStateId;

    private String levelLeftStateId;
    private String levelRightStateId;
    private String localVolumeStateId;
    private String streamVolumeStateId;

    private boolean statesSentToTP;





    public Input(String identifier, String name,
                 Boolean isAvailable, Boolean streamMixerMuted,
                 Boolean localMixerMuted, int localMixerLevel,
                 int streamMixerLevel, ArrayList<InputPlugin> plugins,
                 int inputType, Boolean pluginBypassLocal, Boolean pluginBypassStream) {
        this.identifier = identifier;
        this.name = name;
        this.isAvailable = isAvailable;
        this.streamMixerMuted = streamMixerMuted;
        this.localMixerMuted = localMixerMuted;
        this.localMixerLevel = localMixerLevel;
        this.streamMixerLevel = streamMixerLevel;
        this.plugins = plugins;
        this.inputType = inputType;
        this.pluginBypassLocal = pluginBypassLocal;
        this.pluginBypassStream = pluginBypassStream;
    }


//    public String getStateCategoryId() {
//        return stateCategoryId;
//    }

    public String getLocalMuteStateId() {
        return localMuteStateId;
    }

    public void setLocalMuteStateId(String localMuteStateId) {
        this.localMuteStateId = localMuteStateId;
    }

    public String getStreamMuteStateId() {
        return streamMuteStateId;
    }

    public void setStreamMuteStateId(String streamMuteStateId) {
        this.streamMuteStateId = streamMuteStateId;
    }

    public String getLocalFilterBypassStateId() {
        return localFilterBypassStateId;
    }

    public void setLocalFilterBypassStateId(String localFilterBypassStateId) {
        this.localFilterBypassStateId = localFilterBypassStateId;
    }

    public String getStreamFilterBypassStateId() {
        return streamFilterBypassStateId;
    }

    public void setStreamFilterBypassStateId(String streamFilterBypassStateId) {
        this.streamFilterBypassStateId = streamFilterBypassStateId;
    }

    public String getLevelLeftStateId() {
        return levelLeftStateId;
    }

    public void setLevelLeftStateId(String levelLeftStateId) {
        this.levelLeftStateId = levelLeftStateId;
    }

    public String getLevelRightStateId() {
        return levelRightStateId;
    }

    public void setLevelRightStateId(String levelRightStateId) {
        this.levelRightStateId = levelRightStateId;
    }

    public String getLocalVolumeStateId() {
        return localVolumeStateId;
    }

    public void setLocalVolumeStateId(String localVolumeStateId) {
        this.localVolumeStateId = localVolumeStateId;
    }

    public String getStreamVolumeStateId() {
        return streamVolumeStateId;
    }

    public void setStreamVolumeStateId(String streamVolumeStateId) {
        this.streamVolumeStateId = streamVolumeStateId;
    }

    public boolean isStatesSentToTP() {
        return statesSentToTP;
    }

    public void setStatesSentToTP(boolean statesSentToTP) {
        this.statesSentToTP = statesSentToTP;
    }

    public BigDecimal getLevelLeft() {
        return levelLeft;
    }

    public void setLevelLeft(BigDecimal levelLeft) {
        this.levelLeft = levelLeft;
    }

    public BigDecimal getLevelRight() {
        return levelRight;
    }

    public void setLevelRight(BigDecimal levelRight) {
        this.levelRight = levelRight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }

    public Boolean getStreamMixerMuted() {
        return streamMixerMuted;
    }

    public void setStreamMixerMuted(Boolean streamMixerMuted) {
        this.streamMixerMuted = streamMixerMuted;
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

    public int getStreamMixerLevel() {
        return streamMixerLevel;
    }

    public void setStreamMixerLevel(int streamMixerLevel) {
        this.streamMixerLevel = streamMixerLevel;
    }

    public ArrayList<InputPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(ArrayList<InputPlugin> plugins) {
        this.plugins = plugins;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public Boolean getPluginBypassLocal() {
        return pluginBypassLocal;
    }

    public void setPluginBypassLocal(Boolean pluginBypassLocal) {
        this.pluginBypassLocal = pluginBypassLocal;
    }

    public Boolean getPluginBypassStream() {
        return pluginBypassStream;
    }

    public void setPluginBypassStream(Boolean pluginBypassStream) {
        this.pluginBypassStream = pluginBypassStream;
    }
}
