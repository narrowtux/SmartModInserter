package com.narrowtux.fmm;

import com.google.gson.Gson;
import com.narrowtux.fmm.dirwatch.DirectoryWatchService;
import com.narrowtux.fmm.dirwatch.SimpleDirectoryWatchService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Datastore {
    //singleton stuff
    private static Datastore instance;
    private FileVisitor<Path> fmmScaner = new FMMVisitor();

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

    private Modpack currentModpack = null;
    private int currentTabs = 0;

    public Datastore() {
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

    public void scanDirectory(Path path) throws IOException {
        Files.walkFileTree(path, fmmScaner);
    }

    private String getTabs() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < currentTabs; i++) {
            builder.append('\t');
        }
        return builder.toString();
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

    private class FMMVisitor implements FileVisitor<Path>  {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            System.out.println(getTabs() + "Enter directory " + dir);
            Path modList = dir.resolve("mod-list.json");
            if (!Files.exists(modList) && dir.getParent().equals(getFMMDir())) {
                Modpack.writeModList(modList);
            }
            if (Files.exists(modList)) {
                currentModpack = getModpacks().stream().filter(m -> m.getPath().equals(dir)).findAny().orElseGet(() -> new Modpack(dir.getFileName().toString(), dir));
                System.out.println(getTabs() + "Enter modpack " + currentModpack.getName());
                //remove all mods that are missing in the directory
                currentModpack.getMods().removeAll(currentModpack.getMods().stream().filter(mod -> !Files.exists(mod.getPath())).collect(Collectors.toList()));
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
            if (currentModpack.getMods().stream().filter(mod -> mod.getPath().equals(file)).findAny().isPresent()) {
                return FileVisitResult.CONTINUE;
            }
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
                currentModpack.writeModList();
                Datastore.getInstance().getModpacks().add(currentModpack);
                currentModpack = null;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
