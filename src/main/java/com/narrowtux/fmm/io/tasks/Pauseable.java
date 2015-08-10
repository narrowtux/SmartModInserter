package com.narrowtux.fmm.io.tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public interface Pauseable {
    BooleanProperty paused = new SimpleBooleanProperty(false);

    default boolean isPaused() {
        return paused.get();
    }

    default void setPaused(boolean paused) {
        this.paused.set(paused);
    }

    default void pause() {
        setPaused(true);
    }

    default void unpause() {
        setPaused(false);
    }

    default BooleanProperty pausedProperty() {
        return paused;
    }
}
