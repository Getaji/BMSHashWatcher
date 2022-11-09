package com.getaji.bmshashwatcher.view;

import javafx.scene.control.MenuItem;

public class TypedMenuItem<T> extends MenuItem {
    private T value;

    public TypedMenuItem(String text) {
        super(text);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
