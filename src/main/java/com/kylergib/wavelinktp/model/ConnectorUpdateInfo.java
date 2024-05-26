package com.kylergib.wavelinktp.model;

import java.util.Map;

public class ConnectorUpdateInfo extends Info {
    String pluginId;
    String connectorId;
    Integer value;
    Map<String, Object> data;

    public ConnectorUpdateInfo(String pluginId, String connectorId, Integer value, Map<String, Object> data) {
        this.pluginId = pluginId;
        this.connectorId = connectorId;
        this.value = value;
        this.data = data;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
