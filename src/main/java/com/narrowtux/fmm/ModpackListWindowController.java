package com.narrowtux.fmm;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ModpackListWindowController {
    @FXML
    TreeTableView<Object> modpacks;
    @FXML
    TreeTableColumn<Object, String> nameColumn;
    @FXML
    TreeTableColumn<Object, String> versionColumn;
    @FXML
    TreeTableColumn<Object, Boolean> enabledColumn;
    @FXML
    ProgressBar progress;
    @FXML
    Button playButton;
    @FXML
    TabPane tabPane;

    private Datastore store = Datastore.getInstance();

    public ModpackListWindowController(Stage settingsStage) {
        this.settingsStage = settingsStage;
    }

    Stage settingsStage;

    @FXML
    AnchorPane root;

    TreeItem<Object> treeRoot = new TreeItem<>("root");

    @FXML
    public void initialize() {
        tabPane.getSelectionModel().select(1);
        playButton.setDisable(true);
        modpacks.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv == null) {
                playButton.setDisable(true);
                return;
            }
            playButton.setDisable(!(nv.getValue() instanceof Mod || nv.getValue() instanceof Modpack));
        });
        modpacks.setShowRoot(false);
        modpacks.setRoot(treeRoot);
        nameColumn.setCellValueFactory(features -> {
            Object value = features.getValue().getValue();
            if (value instanceof Modpack) {
                return ((Modpack) value).nameProperty();
            }
            if (value instanceof Mod) {
                return ((Mod) value).nameProperty();
            }
            return null;
        });
        nameColumn.setCellFactory(new Callback<TreeTableColumn<Object, String>, TreeTableCell<Object, String>>() {
            @Override
            public TreeTableCell<Object, String> call(TreeTableColumn<Object, String> objectStringTreeTableColumn) {
                return new TextFieldTreeTableCell<Object, String>(new DefaultStringConverter()) {
                    @Override
                    public void startEdit() {
                        super.startEdit();
                        TreeItem item = modpacks.getTreeItem(getIndex());
                        if (item == null || !(item.getValue() instanceof Modpack)) {
                            cancelEdit();
                        }
                    }
                };
            }
        });
        nameColumn.setEditable(true);
        versionColumn.setCellValueFactory(features -> {
            Object value = features.getValue().getValue();
            if (value instanceof Mod) {
                return ((Mod) value).versionProperty().asString();
            }
            return null;
        });
        enabledColumn.setCellValueFactory(features -> {
            Object value = features.getValue().getValue();
            if (value instanceof Mod) {
                return ((Mod) value).enabledProperty();
            }
            SimpleBooleanProperty simpleBooleanProperty = new SimpleBooleanProperty(false);
            simpleBooleanProperty.addListener(((observableValue, ov, nv) -> {
                if (nv) {
                    simpleBooleanProperty.set(false);
                }
            }));
            return simpleBooleanProperty;
        });
        enabledColumn.setCellFactory(new Callback<TreeTableColumn<Object, Boolean>, TreeTableCell<Object, Boolean>>() {
            @Override
            public TreeTableCell<Object, Boolean> call(TreeTableColumn<Object, Boolean> objectBooleanTreeTableColumn) {
                return new CheckBoxTreeTableCell<Object, Boolean>() {
                    @Override
                    public void updateItem(Boolean value, boolean empty) {
                        super.updateItem(value, empty);

                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        }
                        TreeItem item = modpacks.getTreeItem(getIndex());
                        if (item == null || !(item.getValue() instanceof Mod)) {
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        for (Modpack modpack : store.getModpacks()) {
            treeRoot.getChildren().add(getModpackTreeItem(modpack));
        }

        store.getModpacks().addListener((SetChangeListener<Modpack>) change -> {
                if (change.wasAdded()) {
                    Platform.runLater(() -> treeRoot.getChildren().add(getModpackTreeItem(change.getElementAdded())));
                } else if (change.wasRemoved()) {
                    Platform.runLater(() ->
                            treeRoot.getChildren().stream()
                                    .filter((TreeItem item2) -> item2.getValue() == change.getElementRemoved())
                                    .findAny()
                                    .ifPresent(item3 -> treeRoot.getChildren().remove(item3)));
                }
            });

        progress.setVisible(false);
    }

    private TreeItem<Object> getModpackTreeItem(Modpack modpack) {
        TreeItem<Object> item = new TreeItem<>(modpack);
        for (Mod mod : modpack.getMods()) {
            item.getChildren().add(getModTreeItem(mod));
        }
        modpack.getMods().addListener((ListChangeListener<Mod>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Platform.runLater(() -> change.getAddedSubList().forEach(mod -> item.getChildren().add(getModTreeItem(mod))));
                } else if (change.wasRemoved()) {
                    Platform.runLater(() -> {
                        for (Mod removed : change.getRemoved()) {
                            item.getChildren().stream().filter(item2 -> item2.getValue() == removed).findAny().ifPresent(item3 -> item.getChildren().remove(item3));
                        }
                    });
                }
            }
        });
        return item;
    }

    private TreeItem<Object> getModTreeItem(Mod mod) {
        TreeItem<Object> item = new TreeItem<>(mod);
        return item;
    }


    @FXML
    public void onPlayPressed(ActionEvent event) throws IOException {
        int index = modpacks.getSelectionModel().getFocusedIndex();
        if (index < 0) {
            return;
        }
        TreeItem item = modpacks.getSelectionModel().getModelItem(index);
        Object o = item.getValue();
        Modpack pack = null;
        if (o instanceof Mod) {
            pack = ((Mod) o).getModpack();
        } else if (o instanceof Modpack) {
            pack = ((Modpack) o);
        }
        if (pack != null) {
            ModpackInstaller installer = new ModpackInstaller(pack);
            progress.setVisible(true);
            playButton.setDisable(true);
            progress.progressProperty().bind(installer.progressProperty());
            installer.setOnDone(() -> {
                progress.setVisible(false);
                playButton.setDisable(false);
            });
            installer.setOnError((e) -> {
                progress.setVisible(false);
                playButton.setDisable(false);
            });
            installer.start();
        }
    }

    @FXML
    public void onOpenDataFolderClicked() throws IOException {
        Desktop.getDesktop().open(Datastore.getInstance().getDataDir().toFile());
    }

    @FXML
    public void onSettingsButton() {
        settingsStage.show();
    }
}
