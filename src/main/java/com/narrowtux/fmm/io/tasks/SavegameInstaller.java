package com.narrowtux.fmm.io.tasks;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.util.OS;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Save;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;

public class SavegameInstaller extends ModsInstaller {
    private final Save savegame;

    public SavegameInstaller(Save savegame) {
        super();
        this.savegame = savegame;
    }

    @Override
    protected Collection<ModReference> getMods() {
        return savegame.getMods().stream()
                .filter(mod -> mod != null && mod.getPath() != null && Files.exists(mod.getPath()))
                .map(mod -> new ModReference(mod, null, true))
                .collect(Collectors.toList());
    }

    @Override
    protected Process startFactorio() throws IOException {
        String[] args;
        String exePath = Datastore.getInstance().getFactorioApplication().toString();

        if (OS.isMac()) {
            args = new String[] { "open", exePath, "--args",
                                  "--mp-load-game", savegame.getPath().getFileName().toString() };
        } else {
            args = new String[] { exePath,
                                  "--mp-load-game", savegame.getPath().getFileName().toString() };
        }
        return Runtime.getRuntime().exec(args);
    }
}
