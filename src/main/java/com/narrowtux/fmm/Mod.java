package com.narrowtux.fmm;

import javafx.beans.property.*;
import javafx.scene.control.TreeItem;

import java.nio.file.Path;

public class Mod {
    private StringProperty name = new SimpleStringProperty();
    private StringProperty version = new SimpleStringProperty();
    private BooleanProperty enabled = new SimpleBooleanProperty(true);
    private ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private ObjectProperty<Modpack> modpack = new SimpleObjectProperty<>();

    public Mod(String name, String version, Path path, Modpack modpack) {
        setName(name);
        setVersion(version);
        setPath(path);
        setModpack(modpack);

        enabledProperty().addListener((observableValue, ov, nv) -> {
            getModpack().writeModList();
        });
    }

    public Modpack getModpack() {
        return modpack.get();
    }

    public ObjectProperty<Modpack> modpackProperty() {
        return modpack;
    }

    public void setModpack(Modpack modpack) {
        this.modpack.set(modpack);
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

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getVersion() {
        return version.get();
    }

    public StringProperty versionProperty() {
        return version;
    }

    public void setVersion(String version) {
        this.version.set(version);
    }

    public boolean getEnabled() {
        return enabled.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public String toString() {
        return "Mod{" +
                "name=" + getName() +
                ", version=" + getVersion() +
                '}';
    }

}
