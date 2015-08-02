package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.io.tasks.ModsInstaller;
import com.narrowtux.fmm.io.tasks.SavegameInstaller;
import com.narrowtux.fmm.io.tasks.TaskService;
import com.narrowtux.fmm.model.Save;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

/**
 * Controller for the savegame tab
 */
public class SavesTabController extends TabController {
    private final Datastore store;
    @FXML
    public AnchorPane root;
    Tab tab = new Tab("Saves");
    @FXML
    ListView<Save> saves;

    @Override
    public Tab getTab() {
        return tab;
    }

    public SavesTabController() {
        store = Datastore.getInstance();
    }

    @Override
    public void init() {
        tab.setContent(root);
        saves.setItems(store.getSaves());
        saves.setCellFactory(saveListView -> {
            return new ListCell<Save>() {
                @Override
                protected void updateItem(Save save, boolean empty) {
                    Runnable r = () -> {
                        super.updateItem(save, empty);
                        if (!empty) {
                            setText(null);
                            GridPane pane = new GridPane();
                            pane.setHgap(10);
                            pane.setVgap(4);
                            pane.setPadding(new Insets(0, 10, 0, 10));
                            ImageView screenshot = new ImageView(save.getScreenshot());
                            screenshot.setPreserveRatio(true);
                            screenshot.setFitWidth(75);
                            pane.add(screenshot, 0, 0, 1, 2);
                            Label name = new Label(save.getName());
                            Label fileName = new Label(save.getPath().getFileName().toString());
                            pane.add(name, 1, 0, 1, 1);
                            pane.add(fileName, 1, 1, 1, 1);
                            setGraphic(pane);
                        }
                    };

                    if (Platform.isFxApplicationThread()) {
                        r.run();
                    } else {
                        Platform.runLater(r);
                    }
                }
            };
        });
    }

    @FXML
    public void onPlaySavePressed(ActionEvent actionEvent) {
        Save selected = saves.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ModsInstaller installer = new SavegameInstaller(selected);

            TaskService.getInstance().submit(installer);
        } else {
            System.err.println("No save selected");
        }
    }
}
