package com.narrowtux.fmm;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.ContextMenuEvent;
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
    @FXML TableView<Mod> mods;
    @FXML TableColumn<Mod, String> modsNameColumn;
    @FXML TableColumn<Mod, Version> modsVersionColumn;
    @FXML TableColumn<Mod, Double> modsProgressColumn;

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
            if (value instanceof ModReference) {
                return ((ModReference) value).getMod().nameProperty();
            }
            return null;
        });
        mods.setItems(store.getMods());
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

        modsNameColumn.setCellValueFactory(features -> features.getValue().nameProperty());
        modsVersionColumn.setCellValueFactory(features -> features.getValue().versionProperty());

        mods.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    ContextMenu currentMenu = null;

    @FXML
    public void onModsContext(ContextMenuEvent event) {
        if (currentMenu != null) {
            currentMenu.hide();
        }
        ContextMenu menu = new ContextMenu();

        Menu add = new Menu("Add to modpack ...");
        for (final Modpack pack : store.getModpacks()) {
            MenuItem packItem = new MenuItem(pack.getName());
            packItem.setOnAction(actionEvent -> {
                for (Mod mod : mods.getSelectionModel().getSelectedItems()) {
                    if (!pack.getMods().stream().filter(ref -> ref.getMod().equals(mod)).findAny().isPresent()) {
                        pack.getMods().add(new ModReference(mod, pack, true));
                    }
                }
                pack.writeModList(true);
            });
            add.getItems().add(packItem);
        }

        menu.getItems().add(add);

        MenuItem delete = new MenuItem("Delete");
        //TODO
        menu.getItems().add(delete);

        currentMenu = menu;
        menu.show(mods, event.getScreenX(), event.getScreenY());
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

            TaskService.getInstance().getTasks().add(installer);
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

    @FXML
    public void onModpackAddAction(ActionEvent event) {
        String name = "new-modpack";
        Path path = store.getFMMDir().resolve(name);
        try {
            Files.createDirectory(path);
            Modpack pack = new Modpack(name, path);
            store.getModpacks().add(pack);
            modpacks.getSelectionModel().selectLast();
            modpacks.edit(modpacks.getSelectionModel().getSelectedIndex(), nameColumn);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
