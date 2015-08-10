package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModDependency;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Version;
import com.narrowtux.fmm.util.Bindings;
import com.narrowtux.fmm.util.ObservableMapValues;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
    GridPane modProperties;
    @FXML
    ScrollPane scrollPane;
    @FXML
    StackPane stackPane;
    @FXML
    Label noSelectionLabel;
    @FXML
    Label multiSelectionLabel;
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
                store.getMods().values().stream().filter(Mod::getUnread).forEach(mod -> mod.setUnread(false));
                unreadMods.set(0);
            }
        });

        Datastore.getInstance().getMods().addListener((MapChangeListener<Object, Mod>) change -> {
            int added = 0;
            unreadMods.set((int) store.getMods().values().stream().filter(Mod::getUnread).count());
        });

        tab.setContent(root);
        mods.setItems(new ObservableMapValues<Mod>(store.getMods()));

        modsNameColumn.setCellValueFactory(features -> {
            Mod mod = features.getValue();
            return Bindings.decision(
                    mod.titleProperty().isNotEmpty().and(mod.titleProperty().isNotNull()),
                    mod.titleProperty(),
                    mod.nameProperty());
        });
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
                            setText(s);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                    if (s != null) {
                        setText(s);
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
        setupProperties();
    }

    private void setupProperties() {
        ReadOnlyObjectProperty<Mod> selection = mods.getSelectionModel().selectedItemProperty();

        BooleanBinding isNone = selection.isNull();
        noSelectionLabel.visibleProperty().bind(isNone);
        BooleanBinding isMultiple = Bindings.collectionSize(mods.getSelectionModel().getSelectedCells()).greaterThan(1);
        multiSelectionLabel.visibleProperty().bind(isMultiple);
        modProperties.visibleProperty().bind(isNone.not().and(isMultiple.not()));

        modProperties.prefWidthProperty().bind(scrollPane.widthProperty().subtract(15));

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(Bindings.selection(selection, mod -> Bindings.decision(
                mod.titleProperty().isNotEmpty().and(mod.titleProperty().isNotNull()),
                mod.titleProperty(),
                mod.nameProperty())));
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), 20));
        modProperties.addRow(0, titleLabel);
        GridPane.setColumnSpan(titleLabel, 2);

        Hyperlink authorLink = new Hyperlink();
        authorLink.setPadding(new Insets(0));
        authorLink.textProperty().bind(Bindings.selection(selection, Mod::authorProperty));
        authorLink.setOnAction(e -> {
            String contact = selection.get().getContact();
            if (contact != null) {
                try {
                    Desktop.getDesktop().mail(new URI("mailto:" + contact));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
            authorLink.setVisited(false);
        });
        modProperties.addRow(1, new Label("Author"), authorLink);

        Label versionLabel = new Label();
        versionLabel.textProperty().bind(Bindings.selection(selection, mod -> mod.versionProperty().asString()));
        modProperties.addRow(2, new Label("Version"), versionLabel);

        Hyperlink homepageLink = new Hyperlink("Homepage");
        homepageLink.setPadding(new Insets(0));
        homepageLink.setOnAction(e -> {
            String homepage = selection.get().getHomepage();
            if (homepage != null) {
                try {
                    Desktop.getDesktop().browse(new URI(homepage));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
            homepageLink.setVisited(false);
        });
        modProperties.addRow(3, homepageLink);
        GridPane.setColumnSpan(homepageLink, 2);

        Label descriptionTextArea = new Label();
        descriptionTextArea.textProperty().bind(Bindings.selection(selection, Mod::descriptionProperty));
        descriptionTextArea.setWrapText(true);
        descriptionTextArea.maxWidthProperty().bind(modProperties.widthProperty().subtract(5));
        descriptionTextArea.prefWidthProperty().bind(modProperties.widthProperty().subtract(5));
        modProperties.addRow(4, descriptionTextArea);
        GridPane.setColumnSpan(descriptionTextArea, 2);

        Label dependenciesLabel = new Label("Dependencies");
        modProperties.addRow(5, dependenciesLabel);
        GridPane.setColumnSpan(dependenciesLabel, 2);

        ListView<ModDependency> dependencyList = new ListView<>();
        dependencyList.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dependencyList.setMaxHeight(100);
        dependencyList.itemsProperty().bind(Bindings.selection(selection,
                        mod -> new SimpleObjectProperty<ObservableList<ModDependency>>(mod.getDependencies()),
                        new SimpleObjectProperty<ObservableList<ModDependency>>()));
        modProperties.addRow(6, dependencyList);
        GridPane.setColumnSpan(dependencyList, 2);
    }
}
