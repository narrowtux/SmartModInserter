package com.narrowtux.fmm.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ModDependency {
    private ObjectProperty<Mod> dependentMod = new SimpleObjectProperty<>();
    private StringProperty dependencyName = new SimpleStringProperty();
    private ObjectProperty<MatchedVersion> matchedVersion = new SimpleObjectProperty<>();
    private BooleanProperty optional = new SimpleBooleanProperty(false);

    public ModDependency(Mod dependentMod, String name, MatchedVersion matchedVersion, boolean optional) {
        this.dependentMod.set(dependentMod);
        this.dependencyName.set(name);
        this.matchedVersion.set(matchedVersion);
        this.optional.set(optional);
    }

    public Mod getDependentMod() {
        return dependentMod.get();
    }

    public ObjectProperty<Mod> dependentModProperty() {
        return dependentMod;
    }

    public void setDependentMod(Mod dependentMod) {
        this.dependentMod.set(dependentMod);
    }

    public String getDependencyName() {
        return dependencyName.get();
    }

    public StringProperty dependencyNameProperty() {
        return dependencyName;
    }

    public void setDependencyName(String dependencyName) {
        this.dependencyName.set(dependencyName);
    }

    public MatchedVersion getMatchedVersion() {
        return matchedVersion.get();
    }

    public ObjectProperty<MatchedVersion> matchedVersionProperty() {
        return matchedVersion;
    }

    public void setMatchedVersion(MatchedVersion matchedVersion) {
        this.matchedVersion.set(matchedVersion);
    }

    public boolean getOptional() {
        return optional.get();
    }

    public BooleanProperty optionalProperty() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional.set(optional);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getOptional()) {
            builder.append("? ");
        }
        builder.append(getDependencyName());
        if (getMatchedVersion() != null) {
            builder.append(' ');
            builder.append(getMatchedVersion());
        }
        return builder.toString();
    }
}
