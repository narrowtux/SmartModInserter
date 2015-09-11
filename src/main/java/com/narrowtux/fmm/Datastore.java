package com.narrowtux.fmm;

import com.narrowtux.fmm.dirwatch.DirectoryWatchService;
import com.narrowtux.fmm.dirwatch.SimpleDirectoryWatchService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.io.*;
import java.nio.file.*;
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

    private ObservableSet<Modpack> modpacks = FXCollections.observableSet(new LinkedHashSet<Modpack>());
    private ObjectProperty<Path> dataDir = new SimpleObjectProperty<Path>(null);
    private ObjectProperty<Path> factorioApplication = new SimpleObjectProperty<>();
    private ObjectProperty<Path> storageDir = new SimpleObjectProperty<>();
    private FileVisitor<Path> fmmScaner = new ModpackDetectorVisitor(getModpacks());

    public Datastore() {
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

    public void scanDirectory(Path path) throws IOException {
        Files.walkFileTree(path, fmmScaner);
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

}
