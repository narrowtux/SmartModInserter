package com.narrowtux.fmm.model;

/**
 * Created by tux on 10/08/15.
 */
public class ModKey {
    String name;
    Version version;

    public ModKey(String name, Version version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModKey modKey = (ModKey) o;

        if (!name.equals(modKey.name)) return false;
        return version.equals(modKey.version);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
