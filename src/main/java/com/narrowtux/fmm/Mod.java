package com.narrowtux.fmm;

import javafx.beans.property.*;

import java.nio.file.Path;

public class Mod {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Version> version = new SimpleObjectProperty<>(null);
    private BooleanProperty enabled = new SimpleBooleanProperty(true);
    private ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private ObjectProperty<Modpack> modpack = new SimpleObjectProperty<>();

    public Mod(String name, Version version, Path path, Modpack modpack) {
        setName(name);
        setVersion(version);
        setPath(path);
        setModpack(modpack);

        enabledProperty().addListener((observableValue, ov, nv) -> {
            modpack.writeModList();
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

    public Version getVersion() {
        return version.get();
    }

    public ObjectProperty<Version> versionProperty() {
        return version;
    }

    public void setVersion(Version version) {
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
