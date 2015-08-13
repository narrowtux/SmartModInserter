package com.narrowtux.fmm.io;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.MatchedVersion;
import com.narrowtux.fmm.model.ModDependency;
import com.narrowtux.fmm.util.TreeOutput;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import com.narrowtux.fmm.model.Version;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import javafx.application.Platform;
import org.sat4j.minisat.SolverFactory;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModpackDetectorVisitor implements FileVisitor<Path> {
    private Modpack currentModpack;
    private Collection<Modpack> modpacks;
    private Datastore store;
    private TreeOutput out = new TreeOutput().setMuted(true);

    public ModpackDetectorVisitor(Collection<Modpack> modpacks, Datastore store) {
        this.modpacks = modpacks;
        this.store = store;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        out.println("Enter directory " + dir);
        Path modList = dir.resolve("mod-list.json");
        if (!dir.getFileName().toString().equals("mods") && !dir.getFileName().toString().equals("downloads") && !Files.exists(modList) && dir.getParent().equals(store.getFMMDir())) {
            Modpack.writeModList(modList, false);
        }
        if (!dir.getFileName().toString().equals("mods") && !dir.getFileName().toString().equals("downloads") && Files.exists(modList)) {
            currentModpack = modpacks.stream().filter(m -> m.getPath().equals(dir)).findAny().orElseGet(() -> new Modpack(dir.getFileName().toString(), dir));
            out.println("Enter modpack " + currentModpack.getName());
            //remove all mods that are missing in the directory
            currentModpack.getMods().removeAll(currentModpack.getMods().stream().filter(mod -> !Files.exists(mod.getMod().getPath())).collect(Collectors.toList()));
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
        if (currentModpack.getMods().stream().filter(mod -> mod.getMod().getPath().equals(file)).findAny().isPresent()) {
            return FileVisitResult.CONTINUE;
        }
        if (file.getFileName().toString().endsWith(".zip")) {
            Mod mod = parseMod(file);

            if (mod != null) {
                out.push();
                out.println("Found mod " + mod.toSimpleString());
                out.pull();

                Path oldPath = mod.getPath();
                Path newPath = store.getFMMDir().resolve("mods").resolve(oldPath.getFileName());

                mod.setPath(newPath);

                currentModpack.getMods().add(new ModReference(mod, currentModpack, true));
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

        if (currentModpack != null) {
            // read mod-list.json
            Gson gson = new Gson();
            Map<String, Object> modListData = gson.fromJson(new FileReader(new File(dir.toString(), "mod-list.json")), Map.class);
            List<Map<String, Object>> modList = (List<Map<String, Object>>) modListData.get("mods");
            for (Map<String, Object> mod : modList) {
                String name = (String) mod.get("name");
                String version = null;
                if (mod.containsKey("version")) {
                    version = (String) mod.get("version");
                }
                Object enabledO = mod.get("enabled");
                boolean enabled = enabledO instanceof Boolean ? (Boolean) enabledO : Boolean.valueOf((String) enabledO);
                Optional<ModReference> ref = currentModpack.getMods().stream()
                        .filter(mod1 -> mod1.getMod().getName().equals(name))
                        .findAny();
                ref.ifPresent(mod2 -> mod2.setEnabled(enabled));
                if (!ref.isPresent()) {
                    Mod realMod = null;
                    if (version != null) {
                        realMod = store.getMod(name, Version.valueOf(version));
                    }
                    if (realMod == null) {
                        out.println("could not find mod from mod-list.json: " + name + "#" + version);
                    } else {
                        ModReference reference = new ModReference(realMod, currentModpack, enabled);
                        currentModpack.getMods().add(reference);
                    }
                }
            }
            currentModpack.writeModList(true);
            System.out.println("solutions for " + currentModpack.getName());
            for (Set<Mod> solution : currentModpack.resolveDependencies()) {
                System.out.println(" - " + solution);
            }
            Datastore.getInstance().getModpacks().add(currentModpack);
            currentModpack = null;
        }
        out.pull();
        out.println("Leave");
        return FileVisitResult.CONTINUE;
    }

    private static Pattern MOD_DEPENDENCY_PATTERN = Pattern.compile("(\\??)\\s?([a-zA-Z0-9\\-_]+)\\s?(([>=]+)\\s?([0-9\\.]+))?");

    public static Mod parseMod(Path file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file.toFile());
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry current;
        while ((current = zipInputStream.getNextEntry()) != null) {
            if (current.getName().endsWith("info.json")) {
                break;
            }
        }
        if (current != null) {
            Gson gson = new Gson();
            JsonObject modInfo = null;
            try {
                modInfo = gson.fromJson(new InputStreamReader(zipInputStream), JsonObject.class);
            } catch (JsonSyntaxException e) {
                System.out.println("for file: " + file.toString());
                e.printStackTrace();
                return null;
            } catch (JsonIOException e) {
                e.printStackTrace();
                return null;
            }
            String name = modInfo.get("name").getAsString();
            Version version = Version.valueOf(modInfo.get("version").getAsString());

            Mod mod = Datastore.getInstance().getMod(name, version);
            mod.setPath(file);
            try {
                mod.setTitle(modInfo.get("title").getAsString());
            } catch (Exception ignored) {}
            try {
                mod.setAuthor(modInfo.get("author").getAsString());
            } catch (Exception ignored) {}
            try {
                mod.setContact(modInfo.get("contact").getAsString());
            } catch (Exception ignored) {}
            try {
                mod.setHomepage(modInfo.get("homepage").getAsString());
            } catch (Exception ignored) {}
            try {
                mod.setDescription(modInfo.get("description").getAsString());
            } catch (Exception ignored) {}
            JsonElement dependencies1 = modInfo.get("dependencies");
            if (dependencies1 != null && dependencies1.isJsonArray()) {
                JsonArray dependencies = dependencies1.getAsJsonArray();
                for (JsonElement elem : dependencies) {
                    String depString = elem.getAsString();
                    Matcher matcher = MOD_DEPENDENCY_PATTERN.matcher(depString);
                    if (matcher.matches()) {
                        String optional = matcher.group(1);
                        String depName = matcher.group(2);
                        MatchedVersion matchedVersion = null;
                        if (matcher.group(3) != null) {
                            matchedVersion = MatchedVersion.valueOf(matcher.group(3));
                        }
                        ModDependency dep = new ModDependency(mod, depName, matchedVersion, optional.equals("?"));
                        mod.getDependencies().add(dep);
                    } else {
                        System.out.println("Mod dependency string '" + depString + "' is incorrect.");
                    }
                }
            }

            zipInputStream.closeEntry();
            zipInputStream.close();
            return mod;
        }
        return null;
    }
}
