package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Version;
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
    ContextMenu currentMenu = null;

    public ModsTabController() {
        store = Datastore.getInstance();
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    @FXML
    public void initialize() {
        tab.setContent(root);
        mods.setItems(store.getMods());

        modsNameColumn.setCellValueFactory(features -> features.getValue().nameProperty());
        modsVersionColumn.setCellValueFactory(features -> features.getValue().versionProperty());

        mods.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


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

        menu.getItems().add(delete);

        currentMenu = menu;
        menu.show(mods, event.getScreenX(), event.getScreenY());
    }
}
