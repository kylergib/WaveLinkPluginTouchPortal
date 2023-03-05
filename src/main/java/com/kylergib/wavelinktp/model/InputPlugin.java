package com.kylergib.wavelinktp.model;
/**
 * Plugins/Filters for inputs in wave link
 */
public class InputPlugin {
    private String filterID;
    private String pluginID;
    private String name;
    private Boolean isActive;

    public InputPlugin(String filterID, String pluginID, String name, Boolean isActive) {
        this.filterID = filterID;
        this.pluginID = pluginID;
        this.name = name;
        this.isActive = isActive;
    }

    public void setFilterID(String filterID) {
        this.filterID = filterID;
    }

    public void setPluginID(String pluginID) {
        this.pluginID = pluginID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getFilterID() {
        return this.filterID;
    }
    public String getPluginID() {
        return this.pluginID;
    }
    public String getName() {
        return this.name;
    }
    public Boolean getIsActive() {
        return this.isActive;
    }
    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }


}
