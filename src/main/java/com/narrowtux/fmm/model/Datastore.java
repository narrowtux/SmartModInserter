package com.narrowtux.fmm.model;

import com.narrowtux.fmm.io.dirwatch.DirectoryWatchService;
import com.narrowtux.fmm.io.dirwatch.SimpleDirectoryWatchService;
import com.narrowtux.fmm.io.ModpackDetectorVisitor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class Datastore {
    //singleton stuff
    private static Datastore instance;

    {
        instance = this;
    }

    public static Datastore getInstance() {
        return instance;
    }

    private ObservableSet<Modpack> modpacks = FXCollections.observableSet(new LinkedHashSet<>());
    private ObservableList<Mod> mods = FXCollections.observableList(new ArrayList<>());
    private ObjectProperty<Path> dataDir = new SimpleObjectProperty<>(null);
    private ObjectProperty<Path> factorioApplication = new SimpleObjectProperty<>();
    private ObjectProperty<Path> storageDir = new SimpleObjectProperty<>();
    private ObservableList<Save> saves = FXCollections.observableArrayList();
    private FileVisitor<Path> fmmScaner = new ModpackDetectorVisitor(getModpacks());

    public Datastore() {
        mods.add(new Mod("base", null, null));
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
        Path path = getFMMDir();
        Files.walk(path.resolve("mods")).filter(p -> Files.isRegularFile(p)).filter(p -> p.getFileName().toString().endsWith(".zip"))
                .forEach(modZipFile -> {
                    try {
                        Mod mod = ModpackDetectorVisitor.parseMod(modZipFile);
                        getMods().add(mod);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        Files.walkFileTree(path, fmmScaner);

        Files.walk(getDataDir().resolve("saves")).filter(p -> Files.isRegularFile(p)).filter(p -> p.getFileName().toString().endsWith(".zip"))
                .forEach(saveZipFile -> {
                    Save save = new Save(saveZipFile);
                    saves.add(save);
                });
    }

    public Mod getMod(String name, Version version) {
        return getMods().stream().filter(mod -> mod.getName().equals(name) && version.equals(mod.getVersion())).findAny().orElse(null);
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

    public ObservableList<Mod> getMods() {
        return mods;
    }

    public ObservableList<Save> getSaves() {
        return saves;
    }
}
