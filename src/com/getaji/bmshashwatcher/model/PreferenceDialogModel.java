package com.getaji.bmshashwatcher.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PreferenceDialogModel {
    private final BooleanProperty useBeatorajaDB = new SimpleBooleanProperty();
    private final BooleanProperty useLR2DB = new SimpleBooleanProperty();
    private final StringProperty beatorajaPath = new SimpleStringProperty();
    private final StringProperty lr2Path = new SimpleStringProperty();
    private final ListProperty<WebService> webServices =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    public boolean isUseBeatorajaDB() {
        return useBeatorajaDB.get();
    }

    public BooleanProperty useBeatorajaDBProperty() {
        return useBeatorajaDB;
    }

    public boolean isUseLR2DB() {
        return useLR2DB.get();
    }

    public BooleanProperty useLR2DBProperty() {
        return useLR2DB;
    }

    public String getBeatorajaPath() {
        return beatorajaPath.get();
    }

    public StringProperty beatorajaPathProperty() {
        return beatorajaPath;
    }

    public String getLr2Path() {
        return lr2Path.get();
    }

    public StringProperty lr2PathProperty() {
        return lr2Path;
    }

    public ObservableList<WebService> getWebServices() {
        return webServices.get();
    }

    public ListProperty<WebService> webServicesProperty() {
        return webServices;
    }
}
