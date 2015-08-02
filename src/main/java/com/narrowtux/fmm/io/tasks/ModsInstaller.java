package com.narrowtux.fmm.io.tasks;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.io.FileDeleter;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class ModsInstaller extends Task<Process> {
    private Optional<Runnable> onDone;
    private Optional<Consumer<Exception>> onError;

    protected abstract Collection<ModReference> getMods();

    protected abstract Process startFactorio() throws IOException;

    @Override
    protected Process call() throws Exception {
        updateTitle("Installing mods and launching factorio");
        Process ret = null;
        try {
            int steps = 5 + getMods().size();
            int step = 0;
            updateProgress(step, steps);
            Path tmp = null;
            if (Files.exists(Datastore.getInstance().getModDir())) {
                updateMessage("Backing up old mods directory");
                tmp = Datastore.getInstance().getDataDir().resolve("tmp");
                if (Files.exists(tmp)) {
                    Files.walkFileTree(tmp, new FileDeleter());
                }
                Files.move(Datastore.getInstance().getModDir(), tmp);
                updateProgress(++step, steps);
            }
            updateProgress(++step, steps);
            updateMessage("Creating new mods directory");
            Files.createDirectory(Datastore.getInstance().getModDir());

            updateMessage("Writing mod-list.json");
            Modpack.writeModList(Datastore.getInstance().getModDir().resolve("mod-list.json"), false, getMods().toArray(new ModReference[getMods().size()]));
            updateProgress(++step, steps);
            for (ModReference mod : getMods()) {
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
            ret = startFactorio();
            updateProgress(++step, steps);
            onDone.ifPresent(Platform::runLater);
        } catch (IOException e) {
            e.printStackTrace();
            onError.ifPresent(consumer -> Platform.runLater(() -> consumer.accept(e)));
        }
        return ret;
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