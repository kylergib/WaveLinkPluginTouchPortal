package com.kylergib.wavelinktp.model;

public class StateCreateInfo extends Info {
    String categoryId;
    String stateId;
    Object value;
    String description;
    public StateCreateInfo(String categoryId, String stateId, String description, Object value) {
        this.stateId = stateId;
        this.value = value;
        this.categoryId = categoryId;
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

