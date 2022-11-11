package com.getaji.bmshashwatcher.controller;

import com.getaji.bmshashwatcher.model.WebServiceEditorModel;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class WebServiceEditorController {
    @FXML
    private TextField fieldName;

    @FXML
    private TextField fieldMD5HashPattern;

    @FXML
    private TextField fieldSHA256HashPattern;

    @FXML
    private void initialize() {
    }

    public void bind(WebServiceEditorModel model) {
        fieldName.textProperty().bindBidirectional(model.nameProperty());
        fieldMD5HashPattern.textProperty().bindBidirectional(model.md5UrlPatternProperty());
        fieldSHA256HashPattern.textProperty().bindBidirectional(model.sha256UrlPatternProperty());
    }

    public void unbind(WebServiceEditorModel model) {
        fieldName.textProperty().unbindBidirectional(model.nameProperty());
        fieldMD5HashPattern.textProperty().unbindBidirectional(model.md5UrlPatternProperty());
        fieldSHA256HashPattern.textProperty().unbindBidirectional(model.sha256UrlPatternProperty());
    }
}
