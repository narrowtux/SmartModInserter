package com.narrowtux.fmm.model;

import com.narrowtux.fmm.io.dirwatch.DirectoryWatchService;
import com.narrowtux.fmm.io.dirwatch.SimpleDirectoryWatchService;
import com.narrowtux.fmm.io.ModpackDetectorVisitor;
import com.narrowtux.fmm.io.tasks.TaskService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Datastore {
    //singleton stuff
    private static Datastore instance;

    public static Datastore getInstance() {
        return instance;
    }

    private ObservableSet<Modpack> modpacks = FXCollections.observableSet(new LinkedHashSet<>());
    private ObservableMap<ModKey, Mod> mods = FXCollections.observableMap(new LinkedHashMap<ModKey, Mod>());
    private ObjectProperty<Path> dataDir = new SimpleObjectProperty<>(null);
    private ObjectProperty<Path> factorioApplication = new SimpleObjectProperty<>();
    private ObjectProperty<Path> storageDir = new SimpleObjectProperty<>();
    private ObservableList<Save> saves = FXCollections.observableArrayList();
    private FileVisitor<Path> fmmScaner = new ModpackDetectorVisitor(getModpacks(), this);

    public Datastore() {
        instance = this;
        SimpleDirectoryWatchService.getInstance().start();
        dataDirProperty().addListener((obj, ov, nv) -> {
            if (nv != null) {
                Path fmm = getFMMDir();
                DirectoryWatchService.OnFileChangeListener listener = new DirectoryWatchService.OnFileChangeListener() {
                    @Override
                    public void onFileCreate(String filePath) {
                        try {
                            Files.walkFileTree(getFMMDir().resolve(filePath), fmmScaner);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFileModify(String filePath) {
                        try {
                            Files.walkFileTree(getFMMDir().resolve(filePath), fmmScaner);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFileDelete(String filePath) {
                        Path path = getFMMDir().resolve(filePath);
                        getModpacks().stream().filter(m -> m.getPath().equals(path)).findAny().ifPresent(modpack -> getModpacks().remove(modpack));
                    }
                };
                try {
                    SimpleDirectoryWatchService.getInstance().register(listener, fmm.toAbsolutePath().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void scanDirectory() throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateTitle("Scanning");
                    Path path = getFMMDir();
                    updateProgress(0, 3);
                    updateTitle("Scanning for mods");
                    Files.walk(path.resolve("mods")).filter(p -> Files.isRegularFile(p)).filter(p -> p.getFileName().toString().endsWith(".zip"))
                            .forEach(modZipFile -> {
                                try {
                                    ModpackDetectorVisitor.parseMod(modZipFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                    updateProgress(1, 3);

                    updateTitle("Scanning for modpacks");
                    Files.walkFileTree(path, fmmScaner);
                    updateProgress(2, 3);

                    updateTitle("Scanning for savegames");
                    Files.walk(getDataDir().resolve("saves"))
                            .filter(p -> Files.isRegularFile(p))
                            .filter(p -> p.getFileName().toString().endsWith(".zip"))
                            .forEach(saveZipFile -> {
                                Save save = new Save(saveZipFile);
                                saves.add(save);
                            });
                    updateProgress(3, 3);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }
        };
        TaskService.getInstance().submit(task);
    }

    public Mod getMod(String name, Version version) {
        ModKey key = new ModKey(name, version);
        Mod existing = mods.get(key);
        if (existing == null) {
            existing = new Mod(name, version, null); // create mod without path. This means this mod is not installed
            if (Platform.isFxApplicationThread()) {
                mods.put(key, existing);
            } else {
                final Mod finalExisting = existing;
                Platform.runLater(() -> mods.put(key, finalExisting));
            }
        }
        return existing;
    }

    public ObservableSet<Modpack> getModpacks() {
        return modpacks;
    }

    public Path getDataDir() {
        return dataDir.get();
    }

    public ObjectProperty<Path> dataDirProperty() {
        return dataDir;
    }

    public void setDataDir(Path dataDir) {
        this.dataDir.set(dataDir);
    }

    public Path getModDir() {
        return getDataDir().resolve("mods");
    }

    public Path getFMMDir() {
        return getDataDir().resolve("fmm");
    }

    public Path getFactorioApplication() {
        return factorioApplication.get();
    }

    public ObjectProperty<Path> factorioApplicationProperty() {
        return factorioApplication;
    }

    public void setFactorioApplication(Path factorioApplication) {
        this.factorioApplication.set(factorioApplication);
    }

    public Path getStorageDir() {
        return storageDir.get();
    }

    public ObjectProperty<Path> storageDirProperty() {
        return storageDir;
    }

    public void setStorageDir(Path storageDir) {
        this.storageDir.set(storageDir);
    }

    public ObservableMap<ModKey, Mod> getMods() {
        return mods;
    }

    public ObservableList<Save> getSaves() {
        return saves;
    }
}
