package com.narrowtux.fmm.gui;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Node;

import java.net.URL;
import java.util.ResourceBundle;

public abstract class Controller implements Initializable {
    public abstract Node getRoot();
    public abstract void init();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (Platform.isFxApplicationThread()) {
            init();
        } else {
            Platform.runLater(this::init);
        }
    }
}
