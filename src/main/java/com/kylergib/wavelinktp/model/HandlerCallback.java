package com.kylergib.wavelinktp.model;

public interface HandlerCallback {
    void onStateCreate(StateCreateInfo update);
    void onStateUpdate(StateUpdateInfo update);
    void onConnectorUpdate(ConnectorUpdateInfo update);
}