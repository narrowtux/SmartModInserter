package com.narrowtux.fmm.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ModReference {
    private ObjectProperty<Mod> mod = new SimpleObjectProperty<>();
    private ObjectProperty<Modpack> modpack = new SimpleObjectProperty<>();
    private BooleanProperty enabled = new SimpleBooleanProperty(true);

    public ModReference(Mod mod, Modpack modpack, boolean enabled) {
        this.mod.set(mod);
        this.modpack.set(modpack);
        this.enabled.set(enabled);


        enabledProperty().addListener((observableValue, ov, nv) -> {
            getModpack().writeModList();
        });
    }

    public Mod getMod() {
        return mod.get();
    }

    public ObjectProperty<Mod> modProperty() {
        return mod;
    }

    public void setMod(Mod mod) {
        this.mod.set(mod);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModReference reference = (ModReference) o;

        return !(mod != null ? !mod.equals(reference.mod) : reference.mod != null);

    }

    @Override
    public int hashCode() {
        return mod != null ? mod.hashCode() : 0;
    }
}
