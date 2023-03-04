package com.kylergib.wavelinktp.model;

public class Output {
    private int id = 15;
    private String identifier;
    private String name;
    private Boolean isSelected;

    public Output(String identifier, String name, Boolean isSelected) {
        this.identifier = identifier;
        this.name = name;
        this.isSelected = isSelected;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }
}
