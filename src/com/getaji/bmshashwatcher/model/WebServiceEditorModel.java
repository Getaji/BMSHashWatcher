package com.getaji.bmshashwatcher.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WebServiceEditorModel {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty md5UrlPattern = new SimpleStringProperty();
    private final StringProperty sha256UrlPattern = new SimpleStringProperty();

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getMd5UrlPattern() {
        return md5UrlPattern.get();
    }

    public StringProperty md5UrlPatternProperty() {
        return md5UrlPattern;
    }

    public String getSha256UrlPattern() {
        return sha256UrlPattern.get();
    }

    public StringProperty sha256UrlPatternProperty() {
        return sha256UrlPattern;
    }
}
