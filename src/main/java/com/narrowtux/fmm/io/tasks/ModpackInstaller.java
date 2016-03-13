package com.narrowtux.fmm.io.tasks;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.util.OS;
import com.narrowtux.fmm.model.ModReference;
import com.narrowtux.fmm.model.Modpack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public class ModpackInstaller extends ModsInstaller {
    private final Modpack modpack;

    public ModpackInstaller(Modpack pack) {
        super();
        this.modpack = pack;
    }

    @Override
    protected Collection<ModReference> getMods() {
        return modpack.getMods();
    }

    @Override
    protected Process startFactorio() throws IOException {
        Process process = null;
        Path application = Datastore.getInstance().getFactorioApplication();
        if (OS.isMac()) {
            application = application.resolve("Contents/MacOS/factorio");
        }
        process = Runtime.getRuntime().exec(new String[] { application.toAbsolutePath().toString() });

        return process;
    }
}
