package com.narrowtux.fmm;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.narrowtux.fmm.dirwatch.SimpleDirectoryWatchService;
import com.narrowtux.fmm.gui.MainWindowController;
import com.narrowtux.fmm.gui.SettingsWindowController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FactorioModManagerApplication extends Application {

    private SettingsWindowController settingsWindowController;
    private Stage settingsStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                System.out.println("Exception in main thread: "+throwable.getMessage());
            });

            Datastore store = new Datastore();
            if (OSValidator.isMac()) {
                Path applicationSupport = Paths.get(System.getenv("HOME"), "Library/Application Support/");
                store.setDataDir(applicationSupport.resolve("factorio/"));
                store.setFactorioApplication(Paths.get("/Applications/factorio.app"));
                store.setStorageDir(applicationSupport.resolve("FactorioModManager/"));
            } else if (OSValidator.isWindows()) {
                Path appData = Paths.get(System.getenv("AppData"));
                store.setDataDir(appData.resolve("factorio"));
                store.setStorageDir(appData.resolve("FactorioModManager"));
            } // linux users have to define everything themselves
            Path settingsPath = store.getStorageDir().resolve("settings.json");
            if (Files.exists(settingsPath)) {
                JsonObject settings = new Gson().fromJson(new FileReader(settingsPath.toFile()), JsonObject.class);
                if (settings.has("data")) {
                    store.setDataDir(Paths.get(settings.get("data").getAsString()));
                }
                if (settings.has("executable")) {
                    store.setFactorioApplication(Paths.get(settings.get("executable").getAsString()));
                }
            }
            settingsWindowController = new SettingsWindowController();
            Parent settingsRoot = Util.loadFXML(getClass().getResource("/settingswindow.fxml"), settingsWindowController);
            settingsStage = new Stage();
            settingsStage.setScene(new Scene(settingsRoot));
            settingsWindowController.setWindow(settingsStage);
            //pristine setup
            //check if all the necessary directories have been set
            if (store.getFactorioApplication() == null || store.getDataDir() == null) {
                settingsStage.show();
                settingsWindowController.setOnClose(() -> {
                    try {
                        continueStartup(primaryStage, store);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            if (!Files.exists(store.getStorageDir())) {
                Files.createDirectories(store.getStorageDir());
            }
            continueStartup(primaryStage, store);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void continueStartup(Stage primaryStage, Datastore store) throws IOException {
        //move mod dir to fmm so it can be managed.
        if (!Files.exists(store.getFMMDir())) {
            Files.createDirectory(store.getFMMDir());
            Files.move(store.getModDir(), store.getFMMDir().resolve("default"));
        }
        if (!Files.exists(store.getFMMDir().resolve("mods"))) {
            Files.createDirectory(store.getFMMDir().resolve("mods"));
        }
        store.scanDirectory();

        Parent root = Util.loadFXML(getClass().getResource("/mainwindow.fxml"), new MainWindowController(settingsStage));
        primaryStage.setTitle("Smart Mod Inserter");
        primaryStage.setScene(new Scene(root, 450, 320));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        SimpleDirectoryWatchService.getInstance().stop();
        super.stop();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
