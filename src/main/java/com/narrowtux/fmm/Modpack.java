package com.narrowtux.fmm;

import com.google.gson.*;
import com.google.gson.internal.Excluder;
import com.google.gson.stream.JsonWriter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;

public class Modpack {
    private StringProperty name = new SimpleStringProperty();
    private ObservableList<Mod> mods = FXCollections.observableList(new LinkedList<Mod>());
    private ObjectProperty<Path> path = new SimpleObjectProperty<>();

    public Modpack(String name, Path path) {
        setName(name);
        setPath(path);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public Path getPath() {
        return path.get();
    }

    public ObjectProperty<Path> pathProperty() {
        return path;
    }

    public void setPath(Path path) {
        this.path.set(path);
    }

    public ObservableList<Mod> getMods() {
        return mods;
    }

    @Override
    public String toString() {
        return "Modpack{" +
                "name=" + getName() +
                ", mods=" + mods +
                '}';
    }

    public void writeModList() {
        JsonObject root = new JsonObject();
        JsonArray modList = new JsonArray();
        JsonObject baseMod = new JsonObject();
        baseMod.addProperty("name", "base");
        baseMod.addProperty("enabled", true);
        modList.add(baseMod);
        for (Mod mod : getMods()) {
            JsonObject modInfo = new JsonObject();
            modInfo.addProperty("name", mod.getName());
            modInfo.addProperty("enabled", mod.getEnabled());
            modList.add(modInfo);
        }
        root.add("mods", modList);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(root);
            FileWriter writer = new FileWriter(new File(getPath().toString(), "mod-list.json"));
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
