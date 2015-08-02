package com.narrowtux.fmm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.narrowtux.fmm.gui.GuiFiles;
import com.narrowtux.fmm.gui.ModsTabController;
import com.narrowtux.fmm.gui.SavesTabController;
import com.narrowtux.fmm.io.dirwatch.SimpleDirectoryWatchService;
import com.narrowtux.fmm.gui.MainWindowController;
import com.narrowtux.fmm.gui.SettingsWindowController;
import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.util.OS;
import com.narrowtux.fmm.util.Util;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
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
            Datastore store = new Datastore();
            // preload all GUIs
            settingsWindowController = new SettingsWindowController();
            Util.preloadFXML(GuiFiles.SETTINGS_WINDOW, settingsWindowController);
            Util.preloadFXML(GuiFiles.MAIN_WINDOW);
            Util.preloadFXML(GuiFiles.SAVES_TAB, new SavesTabController());
            Util.preloadFXML(GuiFiles.MODS_TAB, new ModsTabController());
            Util.preloadFXML(GuiFiles.MODPACKS_TAB);

            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                System.out.println("Exception in main thread: " + throwable.getMessage());
            });

            if (OS.isMac()) {
                Path applicationSupport = Paths.get(System.getenv("HOME"), "Library/Application Support/");
                store.setDataDir(applicationSupport.resolve("factorio/"));
                store.setFactorioApplication(Paths.get("/Applications/factorio.app"));
                store.setStorageDir(applicationSupport.resolve("FactorioModManager/"));
            } else if (OS.isWindows()) {
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

            if (store.getFactorioApplication() == null || store.getDataDir() == null) {
                loadSettingsWindow();

                settingsStage.show();
                settingsWindowController.setOnClose(() -> {
                    try {
                        continueStartup(primaryStage, store);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                if (!Files.exists(store.getStorageDir())) {
                    Files.createDirectories(store.getStorageDir());
                }

                continueStartup(primaryStage, store);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void loadSettingsWindow() throws IOException {
        Parent settingsRoot = (Parent) Util.loadFXML(GuiFiles.SETTINGS_WINDOW, () -> settingsWindowController).getNode();
        settingsStage = new Stage();
        settingsStage.setScene(new Scene(settingsRoot));
        settingsWindowController.setWindow(settingsStage);
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

        loadSettingsWindow();

        Parent root = (Parent) Util.loadFXML(GuiFiles.MAIN_WINDOW, () -> new MainWindowController(settingsStage)).getNode();
        primaryStage.setTitle("Smart Mod Inserter");
        primaryStage.setScene(new Scene(root, 450, 320));
        primaryStage.show();
        Util.clearPreloadThreads();
    }

    @Override
    public void stop() throws Exception {
        SimpleDirectoryWatchService.getInstance().stop();
        super.stop();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });
    }
}
