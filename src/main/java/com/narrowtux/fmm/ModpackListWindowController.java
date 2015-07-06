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
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

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
        versionColumn.setCellValueFactory(features -> {
            Object value = features.getValue().getValue();
            if (value instanceof Mod) {
                return ((Mod) value).versionProperty();
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
        enabledColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(enabledColumn));

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
        progress.setVisible(true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
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
                        DoubleProperty step = new SimpleDoubleProperty(0);
                        int steps = 5 + pack.getMods().size();
                        Platform.runLater(() -> progress.progressProperty().bind(step.divide(steps)));
                        Path tmp = null;
                        if (Files.exists(Datastore.getInstance().getModDir())) {
                            tmp = Datastore.getInstance().getDataDir().resolve("tmp");
                            Files.move(Datastore.getInstance().getModDir(), tmp);
                            Platform.runLater(() -> step.set(step.get() + 1));
                        }
                        Platform.runLater(() -> step.set(step.get() + 1));
                        Files.createDirectory(Datastore.getInstance().getModDir());

                        Files.copy(pack.getPath().resolve("mod-list.json"), Datastore.getInstance().getModDir().resolve("mod-list.json"));
                        Platform.runLater(() -> step.set(step.get() + 1));
                        for (Mod mod : pack.getMods()) {
                            Files.copy(mod.getPath(), Datastore.getInstance().getModDir().resolve(mod.getPath().getFileName()));
                            Platform.runLater(() -> step.set(step.get() + 1));
                        }

                        if (tmp != null) {
                            Files.walkFileTree(tmp, new FileDeleter());
                            Platform.runLater(() -> step.set(step.get() + 1));
                        }

                        if (OSValidator.isMac()) {
                            Runtime.getRuntime().exec(new String[]{"open", Datastore.getInstance().getFactorioApplication().toString()});
                        } else {
                            Runtime.getRuntime().exec(Datastore.getInstance().getFactorioApplication().toString());
                        }
                        Platform.runLater(() -> progress.setVisible(false));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
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
