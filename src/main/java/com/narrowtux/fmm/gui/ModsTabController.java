package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Version;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;

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
    StackPane graphic = new StackPane();
    IntegerProperty unreadMods = new SimpleIntegerProperty(0);
    private static final int UNREAD_INDICATOR_WIDTH = 14;

    public ModsTabController() {
        store = Datastore.getInstance();
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    @Override
    public void init() {
        graphic.setAlignment(Pos.CENTER);
        //graphic.setMaxSize(UNREAD_INDICATOR_WIDTH, UNREAD_INDICATOR_WIDTH);
        Circle graphicCircle = new Circle(UNREAD_INDICATOR_WIDTH / 2, Paint.valueOf("red"));
        Label graphicLabel = new Label();
        graphicLabel.setTextFill(Paint.valueOf("white"));
        graphicLabel.textProperty().bind(unreadMods.asString());
        BooleanBinding unreadVisible = unreadMods.greaterThan(0);
        graphic.visibleProperty().bind(unreadVisible);
        graphicCircle.visibleProperty().bind(unreadVisible);
        graphicLabel.visibleProperty().bind(unreadVisible);
//        graphicLabel.setTextAlignment(TextAlignment.CENTER);
//        graphicLabel.setAlignment(Pos.CENTER);
        graphic.getChildren().addAll(graphicCircle, graphicLabel);
        tab.setGraphic(graphic);
        graphicLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        //graphicCircle.setLayoutX(graphicLabel.getLayoutX() + graphicLabel.getWidth() / 2);
        //graphicCircle.setLayoutY(graphicLabel.getLayoutY() + graphicLabel.getHeight() / 2);

        tab.setOnSelectionChanged(event -> {
            if (!tab.isSelected()) {
                store.getMods().stream().filter(Mod::getUnread).forEach(mod -> mod.setUnread(false));
                unreadMods.set(0);
            }
        });

        Datastore.getInstance().getMods().addListener((ListChangeListener<Mod>) change -> {
            int added = 0;
            while (change.next()) {
            }
            unreadMods.set((int) store.getMods().stream().filter(Mod::getUnread).count());
        });

        tab.setContent(root);
        mods.setItems(store.getMods());

        modsNameColumn.setCellValueFactory(features -> features.getValue().nameProperty());
        modsNameColumn.setCellFactory(column -> {
            return new TableCell<Mod, String>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);

                    if (!empty && s != null ) {
                        try {
                            Mod mod = (Mod) getTableRow().getItem();
                            if (mod == null) {
                                setGraphic(null);
                                setText(s);
                                return;
                            }
                            Circle circle = new Circle(5, Paint.valueOf("blue"));
                            circle.setCenterY(circle.getRadius() / 2);
                            circle.visibleProperty().bind(mod.unreadProperty());
                            setGraphic(circle);
                            setText(mod.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            };
        });
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
