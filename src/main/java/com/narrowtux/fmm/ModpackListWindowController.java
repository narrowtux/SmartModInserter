package com.narrowtux.fmm;

import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    TreeTableView modpacks;
    @FXML
    TreeTableColumn<Object, String> nameColumn;
    @FXML
    TreeTableColumn<Object, String> versionColumn;
    @FXML
    TreeTableColumn<Object, Boolean> enabledColumn;

    public ModpackListWindowController(Stage settingsStage) {
        this.settingsStage = settingsStage;
    }

    Stage settingsStage;

    @FXML
    AnchorPane root;

    TreeItem treeRoot = new TreeItem<>("root");

    @FXML
    public void initialize() {
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

        for (Modpack modpack : Datastore.getInstance().getModpacks()) {
            treeRoot.getChildren().add(getModpackTreeItem(modpack));
        }
    }

    private TreeItem<String> getModpackTreeItem(Modpack modpack) {
        TreeItem item = new TreeItem<>(modpack);
        for (Mod mod : modpack.getMods()) {
            item.getChildren().add(getModTreeItem(mod));
        }
        return item;
    }

    private TreeItem<String> getModTreeItem(Mod mod) {
        TreeItem item = new TreeItem<>(mod);
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
            //progress.setVisible(true);
            DoubleProperty step = new SimpleDoubleProperty(0);
            int steps = 5 + pack.getMods().size();
            //progress.progressProperty().bind(step.divide(steps));
            Path tmp = null;
            if (Files.exists(Datastore.getInstance().getModDir())) {
                tmp = Datastore.getInstance().getDataDir().resolve("tmp");
                Files.move(Datastore.getInstance().getModDir(), tmp);
                step.set(step.get() + 1);
            }
            step.set(step.get() + 1);
            Files.createDirectory(Datastore.getInstance().getModDir());

            Files.copy(pack.getPath().resolve("mod-list.json"), Datastore.getInstance().getModDir().resolve("mod-list.json"));
            step.set(step.get() + 1);
            for (Mod mod : pack.getMods()) {
                Files.copy(mod.getPath(), Datastore.getInstance().getModDir().resolve(mod.getPath().getFileName()));
                step.set(step.get() + 1);
            }

            if (tmp != null) {
                Files.walkFileTree(tmp, new FileDeleter());
                step.set(step.get() + 1);
            }

            Runtime.getRuntime().exec(new String[]{"open", Datastore.getInstance().getFactorioApplication().toString()});
            step.set(step.get() + 1);
            //progress.setVisible(false);
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
