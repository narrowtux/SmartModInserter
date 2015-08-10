package com.narrowtux.fmm.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Mod {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Version> version = new SimpleObjectProperty<>(null);
    private ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private BooleanProperty unread = new SimpleBooleanProperty(false);
    private ObservableList<ModDependency> dependencies = FXCollections.observableArrayList();
    private StringProperty title = new SimpleStringProperty();
    private StringProperty author = new SimpleStringProperty();
    private StringProperty contact = new SimpleStringProperty();
    private StringProperty homepage = new SimpleStringProperty();
    private StringProperty description = new SimpleStringProperty();

    public Mod(String name, Version version, Path path) {
        setName(name);
        setVersion(version);
        setPath(path);

        // move file if it has been renamed
        pathProperty().addListener((obj, ov, nv) -> {
            if (nv != null && ov != null && !nv.equals(ov)) {
                try {
                    if (Files.exists(ov) && !Files.exists(nv)) {
                        Files.move(ov, nv);
                    }
                    if (Files.exists(ov) && Files.exists(nv)) {
                        Files.delete(ov);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public ObservableList<ModDependency> getDependencies() {
        return dependencies;
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

    public String toSimpleString() {
        return getName() + '#' + getVersion().toString();
    }

    public boolean getUnread() {
        return unread.get();
    }

    public BooleanProperty unreadProperty() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread.set(unread);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public String getAuthor() {
        return author.get();
    }

    public StringProperty authorProperty() {
        return author;
    }

    public void setAuthor(String author) {
        this.author.set(author);
    }

    public String getContact() {
        return contact.get();
    }

    public StringProperty contactProperty() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact.set(contact);
    }

    public String getHomepage() {
        return homepage.get();
    }

    public StringProperty homepageProperty() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage.set(homepage);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    @Override
    public String toString() {
        return "Mod{" +
                "name=" + getName() +
                ", version=" + getVersion() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mod mod = (Mod) o;

        if (!name.equals(mod.name)) return false;
        return version.equals(mod.version);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
