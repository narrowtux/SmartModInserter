package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.io.tasks.ModDownloadTask;
import com.narrowtux.fmm.io.tasks.TaskService;
import com.narrowtux.fmm.util.Util;
import com.narrowtux.fmm.model.Version;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Arc;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainWindowController extends Controller {
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

    public MainWindowController(Stage settingsStage) {
        this.settingsStage = settingsStage;
    }

    @Override
    public AnchorPane getRoot() {
        return root;
    }

    Stage settingsStage;

    @FXML
    AnchorPane root;

    @Override
    public void init() {
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
            //TaskService.getInstance().getTasks().add(task);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // load all the tabs
        ModsTabController modsTabController;
        ModpackTabController modpackTabController;
        SavesTabController savesTabController;
        try {
            modsTabController = Util.loadFXML(GuiFiles.MODS_TAB, () -> new ModsTabController()).getController();
            modpackTabController = Util.loadFXML(GuiFiles.MODPACKS_TAB, () -> new ModpackTabController(consoleWindow)).getController();
            savesTabController = Util.loadFXML(GuiFiles.SAVES_TAB, () -> new SavesTabController()).getController();

            tabPane.getTabs().addAll(modsTabController.getTab(), modpackTabController.getTab(), savesTabController.getTab());
            tabPane.getSelectionModel().select(modpackTabController.getTab());
        } catch (Exception e) {
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

    @FXML
    public void onOpenDataFolderClicked() throws IOException {
        Desktop.getDesktop().open(Datastore.getInstance().getDataDir().toFile());
    }

    @FXML
    public void onSettingsButton() {
        settingsStage.show();
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
        }
    }
}
