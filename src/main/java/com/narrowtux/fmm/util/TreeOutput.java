package com.narrowtux.fmm.util;

public class TreeOutput {
    private int currentTabs = 0;

    public void push() {
        currentTabs++;
    }

    public void pull() {
        currentTabs--;
    }

    public void println(String text) {
        System.out.println(getTabs() + text);
    }

    private String getTabs() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < currentTabs; i++) {
            builder.append('\t');
        }
        return builder.toString();
    }

}
