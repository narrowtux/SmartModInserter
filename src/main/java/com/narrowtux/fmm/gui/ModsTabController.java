package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Version;
import javafx.application.Platform;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ModsTabController extends TabController {
    private final Datastore store;
    @FXML
    public AnchorPane root;
    Tab tab = new Tab("Mods");
    @FXML
    TableView<Mod> mods;
    @FXML
    TableColumn<Mod, String> modsNameColumn;
    @FXML
    TableColumn<Mod, Version> modsVersionColumn;
    @FXML
    TableColumn<Mod, Double> modsProgressColumn;

    public ModsTabController() {
        store = Datastore.getInstance();
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    @Override
    public void init() {
        tab.setContent(root);
        mods.setItems(store.getMods());

        modsNameColumn.setCellValueFactory(features -> features.getValue().nameProperty());
        modsVersionColumn.setCellValueFactory(features -> features.getValue().versionProperty());

        mods.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu menu = new ContextMenu();

        Menu add = new Menu("Add to modpack ...");
        Consumer<Modpack> onModpackAdded = (pack -> {
            MenuItem packItem = new MenuItem(pack.getName());
            packItem.setOnAction(actionEvent -> {
                mods.getSelectionModel().getSelectedItems().stream().filter(mod -> !pack.getMods().stream().filter(ref -> ref.getMod().equals(mod)).findAny().isPresent()).forEach(mod -> {
                    pack.getMods().add(new ModReference(mod, pack, true));
                });
                pack.writeModList(true);
            });
            Platform.runLater(() -> add.getItems().add(packItem));
        });
        store.getModpacks().forEach(onModpackAdded);
        store.getModpacks().addListener((SetChangeListener<Modpack>) change -> {
            if (change.wasAdded()) {
                onModpackAdded.accept(change.getElementAdded());
            }
            if (change.wasRemoved()) {
                Modpack removed = change.getElementRemoved();
                add.getItems().removeAll(add.getItems().stream().filter(item -> item.getText().equals(removed.getName())).collect(Collectors.toList()));
            }
        });

        menu.getItems().add(add);

        MenuItem delete = new MenuItem("Delete");

        menu.getItems().add(delete);
        mods.setContextMenu(menu);
    }
}
