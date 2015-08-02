package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.Datastore;
import com.narrowtux.fmm.ModDownloadTask;
import com.narrowtux.fmm.ModpackInstaller;
import com.narrowtux.fmm.ModsInstaller;
import com.narrowtux.fmm.SavegameInstaller;
import com.narrowtux.fmm.TaskService;
import com.narrowtux.fmm.gui.ConsoleWindow;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Save;
import com.narrowtux.fmm.model.Version;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Arc;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;

public class ModpackListWindowController extends Controller {
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
    @FXML
    Arc globalProgress;
    @FXML
    Button globalProgressButton;


    private TaskProgressView taskProgressView = new TaskProgressView();
    private PopOver progressPopover = new PopOver(taskProgressView);

    ConsoleWindow consoleWindow = new ConsoleWindow();
    private Datastore store = Datastore.getInstance();

    public ModpackListWindowController(Stage settingsStage) {
        this.settingsStage = settingsStage;
    }

    @Override
    public AnchorPane getRoot() {
        return root;
    }

    Stage settingsStage;

    @FXML
    AnchorPane root;
    @FXML TableView<Mod> mods;
    @FXML TableColumn<Mod, String> modsNameColumn;
    @FXML TableColumn<Mod, Version> modsVersionColumn;
    @FXML TableColumn<Mod, Double> modsProgressColumn;

    TreeItem<Object> treeRoot = new TreeItem<>("root");

    @FXML ListView<Save> saves;

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

        saves.setItems(store.getSaves());
        saves.setCellFactory(saveListView -> {
            return new ListCell<Save>() {
                @Override
                protected void updateItem(Save save, boolean empty) {
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
                }
            };
        });

        progressPopover.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
        globalProgress.startAngleProperty().bind(globalProgress.lengthProperty().negate().add(90));
        globalProgress.setLength(360);
        TaskService.getInstance().getTasks().addListener((ListChangeListener<Task>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Task task : change.getAddedSubList()) {
                        taskProgressView.getTasks().add(task);
                        task.progressProperty().addListener((obs, ov, nv) -> updateGlobalProgress(nv.equals(1d)));
                    }
                }
                if (change.wasRemoved()) {
                    for (Task task : change.getRemoved()) {
                        taskProgressView.getTasks().remove(task);
                    }
                }
            }
        });

        try {
            ModDownloadTask task = new ModDownloadTask(new URL("file:///Users/tux/Downloads/FARL_0.2.8.zip"), "FARL", Version.valueOf("0.2.8"));
            TaskService.getInstance().getTasks().add(task);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private long lastGlobalProgressUpdate = 0;
    private void updateGlobalProgress(boolean force) {
        if (force || System.currentTimeMillis() - lastGlobalProgressUpdate > 100) {
            int runningTasks = 0;
            int tasksWaiting = 0;
            double progress = 0;
            for (Task task : TaskService.getInstance().getTasks()) {
                if (task.isRunning()) {
                    runningTasks ++;
                    if (task.getWorkDone() >= 0) {
                        progress += task.getProgress();
                    } else {
                        tasksWaiting ++;
                    }
                }
            }
            double length;
            if (tasksWaiting == runningTasks || runningTasks == 0) {
                length = 360;
            } else {
                length = 360d * (progress / (double) runningTasks);
            }

            globalProgress.setLength(length);

            lastGlobalProgressUpdate = System.currentTimeMillis();
        }
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
            Platform.runLater(() -> modpacks.edit(modpacks.getSelectionModel().getSelectedIndex(), nameColumn));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void onPlaySavePressed(ActionEvent actionEvent) {
        Save selected = saves.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ModsInstaller installer = new SavegameInstaller(selected);

            TaskService.getInstance().getTasks().add(installer);
        } else {
            System.err.println("No save selected");
        }
    }

    @FXML
    public void onConsoleAction(ActionEvent event) {
        consoleWindow.show();
    }


    @FXML
    public void onProgressAction(ActionEvent event) {
        if (progressPopover.isShowing()) {
            progressPopover.hide();
        } else {
            progressPopover.show(globalProgressButton);

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    int progress = 0;
                    int total = 6000;
                    updateTitle("Super important task");
                    updateMessage("waiting 1 seconds");
                    updateProgress(-1, 1);
                    Thread.sleep(1000);
                    while (progress != total) {
                        updateProgress(progress, total);
                        updateMessage("AH + " + progress);
                        progress++;
                        Thread.sleep(10);
                    }
                    return null;
                }
            };
            TaskService.getInstance().getTasks().add(task);
        }
    }
}
