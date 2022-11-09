package com.getaji.bmshashwatcher;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PreferenceDialogModel {
    private final BooleanProperty useBeatorajaDB = new SimpleBooleanProperty();
    private final BooleanProperty useLR2DB = new SimpleBooleanProperty();
    private final StringProperty beatorajaPath = new SimpleStringProperty();
    private final StringProperty lr2Path = new SimpleStringProperty();

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
}
