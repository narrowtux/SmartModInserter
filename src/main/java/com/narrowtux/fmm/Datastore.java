package com.narrowtux.fmm;

import com.google.gson.Gson;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Datastore {
    //singleton stuff
    private static Datastore instance;

    {
        instance = this;
    }

    public static Datastore getInstance() {
        return instance;
    }

    private ObservableList<Modpack> modpacks = FXCollections.observableList(new LinkedList<Modpack>());
    private ObjectProperty<Path> dataDir = new SimpleObjectProperty<Path>(null);
    private ObjectProperty<Path> factorioApplication = new SimpleObjectProperty<>();
    private ObjectProperty<Path> storageDir = new SimpleObjectProperty<>();

    private Modpack currentModpack = null;
    private int currentTabs = 0;

    public void scanDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println(getTabs() + "Enter directory " + dir);
                if (Files.exists(Paths.get(dir.toString(), "mod-list.json"))) {
                    currentModpack = new Modpack(dir.getFileName().toString(), dir);
                    System.out.println(getTabs() + "Enter modpack " + currentModpack.getName());
                    currentTabs++;
                    return FileVisitResult.CONTINUE;
                }
                //check if we are in the root directory because this will be passed to us once.
                if (dir.getFileName().toString().equals("fmm")) {
                    currentTabs++;
                    return FileVisitResult.CONTINUE;
                }
                //skip directories without a mod-list.json file, because those aren't modpacks
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (currentModpack == null) {
                    return FileVisitResult.CONTINUE;
                }
                System.out.println(getTabs() + "Checking file " + file.toString());
                if (file.getFileName().toString().endsWith(".zip")) {
                    FileInputStream inputStream = new FileInputStream(file.toFile());
                    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                    ZipEntry current;
                    currentTabs++;
                    while ((current = zipInputStream.getNextEntry()) != null) {
                        if (current.getName().endsWith("info.json")) {
                            break;
                        }
                    }
                    if (current != null) {
                        Gson gson = new Gson();
                        Map<String, Object> modInfo = gson.fromJson(new InputStreamReader(zipInputStream), Map.class);
                        Mod mod = new Mod(((String) modInfo.get("name")), ((String) modInfo.get("version")), file, currentModpack);
                        currentModpack.getMods().add(mod);

                        System.out.println(getTabs() + "Found mod: " + mod.toString());
                        zipInputStream.closeEntry();
                        zipInputStream.close();
                    }
                    currentTabs--;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                currentTabs--;
                System.out.println(getTabs() + "Leave");
                if (currentModpack != null) {
                    // read mod-list.json
                    Gson gson = new Gson();
                    Map<String, Object> modListData = gson.fromJson(new FileReader(new File(dir.toString(), "mod-list.json")), Map.class);
                    List<Map<String, Object>> modList = (List<Map<String, Object>>) modListData.get("mods");
                    for (Map<String, Object> mod : modList) {
                        String name = (String) mod.get("name");
                        Object enabledO = mod.get("enabled");
                        boolean enabled = enabledO instanceof Boolean ? (Boolean) enabledO : Boolean.valueOf((String) enabledO);
                        currentModpack.getMods().stream()
                                .filter(mod1 -> mod1.getName().equals(name))
                                .findAny()
                                    .ifPresent(mod2 -> mod2.setEnabled(enabled));
                    }

                    Datastore.getInstance().getModpacks().add(currentModpack);
                    currentModpack = null;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getTabs() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < currentTabs; i++) {
            builder.append('\t');
        }
        return builder.toString();
    }

    public ObservableList<Modpack> getModpacks() {
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
