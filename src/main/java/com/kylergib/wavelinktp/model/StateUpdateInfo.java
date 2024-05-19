package com.kylergib.wavelinktp.model;

public class StateUpdateInfo extends Info {
    String stateId;
    Object value;
    public StateUpdateInfo(String stateId, Object value) {
        this.stateId = stateId;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }
}
