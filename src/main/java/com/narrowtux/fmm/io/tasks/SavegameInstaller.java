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
        Process process = null;
        if (OS.isMac()) {
            process = Runtime.getRuntime().exec(new String[]{"open", Datastore.getInstance().getFactorioApplication().toString(), "--args", "--mp-load-game", savegame.getPath().getFileName().toString()});
        } else {
            process = Runtime.getRuntime().exec(Datastore.getInstance().getFactorioApplication().toString());
        }
        return process;
    }
}
