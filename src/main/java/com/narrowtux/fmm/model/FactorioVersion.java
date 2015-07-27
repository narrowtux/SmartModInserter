package com.narrowtux.fmm.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;

public class FactorioVersion {
    private ObjectProperty<Version> version = new SimpleObjectProperty<>();
    private ObjectProperty<Path> executable = new SimpleObjectProperty<>();
    private ObjectProperty<Path> dataFolder = new SimpleObjectProperty<>();
    private StringProperty name = new SimpleStringProperty();

    public Version getVersion() {
        return version.get();
    }

    public ObjectProperty<Version> versionProperty() {
        return version;
    }

    public void setVersion(Version version) {
        this.version.set(version);
    }

    public Path getExecutable() {
        return executable.get();
    }

    public ObjectProperty<Path> executableProperty() {
        return executable;
    }

    public void setExecutable(Path executable) {
        this.executable.set(executable);
    }

    public Path getDataFolder() {
        return dataFolder.get();
    }

    public ObjectProperty<Path> dataFolderProperty() {
        return dataFolder;
    }

    public void setDataFolder(Path dataFolder) {
        this.dataFolder.set(dataFolder);
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
}
