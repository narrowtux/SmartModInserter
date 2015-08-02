package com.narrowtux.fmm.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.narrowtux.fmm.Datastore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsWindowController extends Controller {
    @FXML
    VBox root;

    @FXML
    Button executableButton;

    @FXML
    Button closeButton;

    @FXML
    Button dataButton;

    private Runnable onClose;

    public Runnable getOnClose() {
        return onClose;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public VBox getRoot() {
        return root;
    }

    @FXML
    public void initialize() {
        Datastore.getInstance().dataDirProperty().addListener((observableValue, ov, nv) -> {
            updateButton(dataButton, nv);
        });
        Datastore.getInstance().factorioApplicationProperty().addListener((obs, ov, nv) -> {
            updateButton(executableButton, nv);
        });

        updateButton(dataButton, Datastore.getInstance().getDataDir());
        updateButton(executableButton, Datastore.getInstance().getFactorioApplication());
    }

    public void setWindow(Window window) {
        window.setOnCloseRequest(event -> {
            if (closeButton.isDisable()) {
                event.consume();
            } else {
                try {
                    onClose();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateButton(Button button, Path path) {
        if (path != null) {
            button.setText(".../" + path.getFileName().toString());
        } else {
            button.setText("...");
        }
        closeButton.setDisable(Datastore.getInstance().getDataDir() == null || Datastore.getInstance().getFactorioApplication() == null);
    }

    @FXML
    public void onClose() throws IOException {
        Gson gson = new Gson();
        JsonObject settings = new JsonObject();
        settings.addProperty("data", Datastore.getInstance().getDataDir().toString());
        settings.addProperty("executable", Datastore.getInstance().getFactorioApplication().toString());
        FileWriter out = new FileWriter(Datastore.getInstance().getStorageDir().resolve("settings.json").toFile());
        gson.toJson(settings, new JsonWriter(out));
        out.close();
        root.getScene().getWindow().hide();
        if (onClose != null) {
            onClose.run();
            onClose = null;
        }
    }

    @FXML
    public void onSelectExecutableButton(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        Path factorioApplication = Datastore.getInstance().getFactorioApplication();
        if (factorioApplication != null) {
            chooser.setInitialDirectory(factorioApplication.getParent().toFile());
        }
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Datastore.getInstance().setFactorioApplication(Paths.get(file.getAbsolutePath()));
    }

    @FXML
    public void onSelectDataButton(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(Datastore.getInstance().getDataDir().toFile());
        File dir = chooser.showDialog(root.getScene().getWindow());
        if (dir == null) {
            return;
        }
        Datastore.getInstance().setDataDir(Paths.get(dir.getAbsolutePath()));
    }
}
