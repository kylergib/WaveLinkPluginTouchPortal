package com.kylergib.wavelinktp.model;

public interface WaveLinkCallback {
    void onConfigsReceived();
    void onConnectedToWrongApp();
    void onConnectedToWaveLink();
    void onWaveLinkDisconnected();
}
