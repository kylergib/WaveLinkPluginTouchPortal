package com.kylergib.wavelinktp.model;

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
