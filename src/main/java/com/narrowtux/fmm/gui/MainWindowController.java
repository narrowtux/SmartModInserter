package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.Datastore;
import com.narrowtux.fmm.ModDownloadTask;
import com.narrowtux.fmm.ModpackInstaller;
import com.narrowtux.fmm.ModsInstaller;
import com.narrowtux.fmm.SavegameInstaller;
import com.narrowtux.fmm.TaskService;
import com.narrowtux.fmm.Util;
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
import javafx.fxml.FXMLLoader;
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

    @FXML
    public void initialize() {
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
        ModsTabController modsTabController = new ModsTabController();
        ModpackTabController modpackTabController = new ModpackTabController(consoleWindow);
        SavesTabController savesTabController = new SavesTabController();
        try {
            Util.loadFXML("/modstab.fxml", modsTabController);
            Util.loadFXML("/modpackstab.fxml", modpackTabController);
            Util.loadFXML("/savestab.fxml", savesTabController);

            tabPane.getTabs().addAll(modsTabController.getTab(), modpackTabController.getTab(), savesTabController.getTab());
            tabPane.getSelectionModel().select(modpackTabController.getTab());
        } catch (IOException e) {
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
