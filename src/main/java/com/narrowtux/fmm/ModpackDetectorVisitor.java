package com.narrowtux.fmm;

import com.google.gson.Gson;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by tux on 06.07.15.
 */
class ModpackDetectorVisitor implements FileVisitor<Path> {
    private Modpack currentModpack;
    private Collection<Modpack> modpacks;
    private Datastore store = Datastore.getInstance();
    private TreeOutput out = new TreeOutput();

    public ModpackDetectorVisitor(Collection<Modpack> modpacks) {
        this.modpacks = modpacks;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        out.println("Enter directory " + dir);
        Path modList = dir.resolve("mod-list.json");
        if (!Files.exists(modList) && dir.getParent().equals(store.getFMMDir())) {
            Modpack.writeModList(modList);
        }
        if (Files.exists(modList)) {
            currentModpack = modpacks.stream().filter(m -> m.getPath().equals(dir)).findAny().orElseGet(() -> new Modpack(dir.getFileName().toString(), dir));
            out.println("Enter modpack " + currentModpack.getName());
            //remove all mods that are missing in the directory
            currentModpack.getMods().removeAll(currentModpack.getMods().stream().filter(mod -> !Files.exists(mod.getPath())).collect(Collectors.toList()));
            out.push();
            return FileVisitResult.CONTINUE;
        }
        //check if we are in the root directory because this will be passed to us once.
        if (dir.getFileName().toString().equals("fmm")) {
            out.push();
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
        out.println("Checking file " + file.toString());
        if (currentModpack.getMods().stream().filter(mod -> mod.getPath().equals(file)).findAny().isPresent()) {
            return FileVisitResult.CONTINUE;
        }
        if (file.getFileName().toString().endsWith(".zip")) {
            FileInputStream inputStream = new FileInputStream(file.toFile());
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry current;
            out.push();
            while ((current = zipInputStream.getNextEntry()) != null) {
                if (current.getName().endsWith("info.json")) {
                    break;
                }
            }
            if (current != null) {
                Gson gson = new Gson();
                Map<String, Object> modInfo = gson.fromJson(new InputStreamReader(zipInputStream), Map.class);
                Mod mod = new Mod(((String) modInfo.get("name")), Version.valueOf((String) modInfo.get("version")), file, currentModpack);
                currentModpack.getMods().add(mod);

                out.println("Found mod: " + mod.toString());
                zipInputStream.closeEntry();
                zipInputStream.close();
            }
            out.pull();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        out.pull();
        out.println("Leave");
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
