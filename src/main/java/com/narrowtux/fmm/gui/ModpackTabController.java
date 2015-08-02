package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.io.tasks.ModpackInstaller;
import com.narrowtux.fmm.io.tasks.ModsInstaller;
import com.narrowtux.fmm.io.tasks.TaskService;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for the modpacks tab
 */
public class ModpackTabController extends TabController {
    @FXML
    public AnchorPane root;
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
    Tab tab = new Tab("Modpacks");

    private final Datastore store;
    private final ConsoleWindow consoleWindow;
    TreeItem<Object> treeRoot = new TreeItem<>("root");

    public ModpackTabController(ConsoleWindow consoleWindow) {
        this.consoleWindow = consoleWindow;
        store = Datastore.getInstance();
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    @Override
    public void init() {
        tab.setContent(root);

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
            if (value instanceof ModReference) {
                return ((ModReference) value).getMod().nameProperty();
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
            if (value instanceof ModReference) {
                return ((ModReference) value).getMod().versionProperty().asString();
            }
            return null;
        });
        enabledColumn.setCellValueFactory(features -> {
            Object value = features.getValue().getValue();
            if (value instanceof ModReference) {
                return ((ModReference) value).enabledProperty();
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
                        if (item == null || !(item.getValue() instanceof ModReference)) {
                            setGraphic(null);
                        }
                    }
                };
            }
        });
    }

    @FXML
    public void onModpackAddAction(ActionEvent event) {
        String name = "new-modpack";
        Path path = store.getFMMDir().resolve(name);
        try {
            Files.createDirectory(path);
            Modpack pack = new Modpack(name, path);
            store.getModpacks().add(pack);
            modpacks.getSelectionModel().selectLast();
            Platform.runLater(() -> modpacks.edit(modpacks.getSelectionModel().getSelectedIndex(), nameColumn));
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        if (o instanceof ModReference) {
            pack = ((ModReference) o).getModpack();
        } else if (o instanceof Modpack) {
            pack = ((Modpack) o);
        }
        if (pack != null) {
            ModsInstaller installer = new ModpackInstaller(pack);
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
            installer.valueProperty().addListener((obj, ov, nv) -> {
                if (nv != null) {
                    consoleWindow.getProcesses().add(nv);
                }
            });

            TaskService.getInstance().submit(installer);
        }
    }

    private TreeItem<Object> getModpackTreeItem(Modpack modpack) {
        TreeItem<Object> item = new TreeItem<>(modpack);
        for (ModReference mod : modpack.getMods()) {
            item.getChildren().add(getModTreeItem(mod));
        }
        modpack.getMods().addListener((SetChangeListener<ModReference>) change -> {
            if (change.wasAdded()) {
                Platform.runLater(() -> item.getChildren().add(getModTreeItem(change.getElementAdded())));
            } else if (change.wasRemoved()) {
                Platform.runLater(() -> {
                    item.getChildren().stream().filter(item2 -> item2.getValue() == change.getElementRemoved()).findAny().ifPresent(item3 -> item.getChildren().remove(item3));
                });
            }
        });
        return item;
    }

    private TreeItem<Object> getModTreeItem(ModReference mod) {
        TreeItem<Object> item = new TreeItem<>(mod);
        return item;
    }
}
