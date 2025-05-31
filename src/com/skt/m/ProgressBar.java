package com.skt.m;

public final class ProgressBar {
    private int value = 0;
    private int maxValue = 100;

    public ProgressBar(String title) {

    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setMaxValue(int maxValue2) {
        this.maxValue = maxValue2;
    }

    public int getMaxValue() {
        return this.maxValue;
    }
}
