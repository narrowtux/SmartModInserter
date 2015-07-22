package com.narrowtux.fmm;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class ModpackInstaller extends Task<Void> {
    private Modpack modpack;
    private Optional<Runnable> onDone;
    private Optional<Consumer<Exception>> onError;

    public ModpackInstaller(Modpack modpack) {
        this.modpack = modpack;
    }

    @Override
    protected Void call() throws Exception {
        try {
            int steps = 5 + modpack.getMods().size();
            int step = 0;
            updateProgress(step, steps);
            Path tmp = null;
            if (Files.exists(Datastore.getInstance().getModDir())) {
                updateMessage("Backing up old mods directory");
                tmp = Datastore.getInstance().getDataDir().resolve("tmp");
                Files.move(Datastore.getInstance().getModDir(), tmp);
                updateProgress(++step, steps);
            }
            updateProgress(++step, steps);
            updateMessage("Creating new mods directory");
            Files.createDirectory(Datastore.getInstance().getModDir());

            updateMessage("Writing mod-list.json");
            Files.copy(modpack.getPath().resolve("mod-list.json"), Datastore.getInstance().getModDir().resolve("mod-list.json"));
            updateProgress(++step, steps);
            for (ModReference mod : modpack.getMods()) {
                updateMessage("Installing mod " + mod.getMod().toSimpleString());
                Files.copy(mod.getMod().getPath(), Datastore.getInstance().getModDir().resolve(mod.getMod().getPath().getFileName()));
                updateProgress(++step, steps);
            }

            updateMessage("Removing backup");
            if (tmp != null) {
                Files.walkFileTree(tmp, new FileDeleter());
                updateProgress(++step, steps);
            }

            updateMessage("Launching factorio");
            if (OSValidator.isMac()) {
                Runtime.getRuntime().exec(new String[]{"open", Datastore.getInstance().getFactorioApplication().toString()});
            } else {
                Runtime.getRuntime().exec(Datastore.getInstance().getFactorioApplication().toString());
            }
            updateProgress(++step, steps);
            onDone.ifPresent(Platform::runLater);
        } catch (IOException e) {
            e.printStackTrace();
            onError.ifPresent(consumer -> Platform.runLater(() -> consumer.accept(e)));
        }
        return null;
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
}