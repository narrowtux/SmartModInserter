package com.narrowtux.fmm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
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
        } else if (OSValidator.isUnix()) {
            //TODO tell them to specify their installation directory
        }
        //pristine setup
        //move mod dir to fmm so it can be managed.
        if (!Files.exists(store.getFMMDir())) {
            Files.createDirectory(store.getFMMDir());
            Files.move(store.getModDir(), store.getFMMDir().resolve("default"));
        }
        store.scanDirectory(store.getFMMDir());

        Parent root = FXMLLoader.load(getClass().getResource("/modpackwindow.fxml"));
        primaryStage.setTitle("Factorio Modpacks");
        primaryStage.setScene(new Scene(root, 450, 320));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
