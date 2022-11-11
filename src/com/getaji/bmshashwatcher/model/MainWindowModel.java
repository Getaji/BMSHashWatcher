package com.getaji.bmshashwatcher.model;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MainWindowModel {
    private final ListProperty<BMSHashData> hashList =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    public ObservableList<BMSHashData> getHashList() {
        return hashList.get();
    }

    public ListProperty<BMSHashData> hashListProperty() {
        return hashList;
    }
}
