package com.narrowtux.fmm;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class ModpackInstaller extends Thread {
    private DoubleProperty progress = new SimpleDoubleProperty();
    private Modpack modpack;
    private Optional<Runnable> onDone;
    private Optional<Consumer<Exception>> onError;

    public ModpackInstaller(Modpack modpack) {
        this.modpack = modpack;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public Optional<Consumer<Exception>> getOnError() {
        return onError;
    }

    public void setOnError(Consumer<Exception> onError) {
        this.onError = Optional.ofNullable(onError);
    }

    public Optional<Runnable> getOnDone() {
        return onDone;
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = Optional.ofNullable(onDone);
    }

    @Override
    public void run() {
        try {
            DoubleProperty step = new SimpleDoubleProperty(0);
            int steps = 5 + modpack.getMods().size();
            Platform.runLater(() -> progress.bind(step.divide(steps)));
            Path tmp = null;
            if (Files.exists(Datastore.getInstance().getModDir())) {
                tmp = Datastore.getInstance().getDataDir().resolve("tmp");
                Files.move(Datastore.getInstance().getModDir(), tmp);
                Platform.runLater(() -> step.set(step.get() + 1));
            }
            Platform.runLater(() -> step.set(step.get() + 1));
            Files.createDirectory(Datastore.getInstance().getModDir());

            Files.copy(modpack.getPath().resolve("mod-list.json"), Datastore.getInstance().getModDir().resolve("mod-list.json"));
            Platform.runLater(() -> step.set(step.get() + 1));
            for (Mod mod : modpack.getMods()) {
                Files.copy(mod.getPath(), Datastore.getInstance().getModDir().resolve(mod.getPath().getFileName()));
                Platform.runLater(() -> step.set(step.get() + 1));
            }

            if (tmp != null) {
                Files.walkFileTree(tmp, new FileDeleter());
                Platform.runLater(() -> step.set(step.get() + 1));
            }

            if (OSValidator.isMac()) {
                Runtime.getRuntime().exec(new String[]{"open", Datastore.getInstance().getFactorioApplication().toString()});
            } else {
                Runtime.getRuntime().exec(Datastore.getInstance().getFactorioApplication().toString());
            }
            Platform.runLater(() -> step.set(step.get() + 1));
            onDone.ifPresent(Platform::runLater);
        } catch (IOException e) {
            e.printStackTrace();
            onError.ifPresent(consumer -> Platform.runLater(() -> consumer.accept(e)));
        }
    }
}