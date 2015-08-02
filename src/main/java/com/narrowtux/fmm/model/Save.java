package com.narrowtux.fmm.model;

import com.google.common.io.LittleEndianDataInputStream;
import com.narrowtux.fmm.util.Util;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Save {
    private ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Image> screenshot = new SimpleObjectProperty<>();
    private ObservableList<Mod> mods = FXCollections.observableArrayList();

    //TODO more properties like playtime and such

    public Save(Path path) {
        this.path.addListener((obj, ov, nv) -> {
            if (nv != null) {
                // set the filename
                String fileName = nv.getFileName().toString();
                fileName = fileName.substring(0, fileName.indexOf('.'));
                name.set(fileName);

                mods.clear();
                screenshot.set(null);
                try {
                    ZipInputStream zip = new ZipInputStream(new FileInputStream(nv.toFile()));
                    ZipEntry entry = null;
                    while ((entry = zip.getNextEntry()) != null) {
                        String zipEntryName = (new File(entry.getName()).getName());
                        if (zipEntryName.equals("level-init.dat")) {
                            // read mods
                            LittleEndianDataInputStream in = new LittleEndianDataInputStream(new BufferedInputStream(zip));
                            // skip 32 bytes and then look for "base"
                            in.skip(32);
                            in.mark(64);

                            int bytes = 0;
                            while (true) {
                                char c = (char) in.readUnsignedByte();
                                bytes ++;
                                if (c == 'b') {
                                    c = (char) in.readUnsignedByte();
                                    bytes ++;
                                    if (c == 'a') {
                                        c = (char) in.readUnsignedByte();
                                        bytes ++;
                                        if (c == 's') {
                                            c = (char) in.readUnsignedByte();
                                            bytes ++;
                                            if (c == 'e') {
                                                in.reset();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            // word "base" was found
                            bytes -= 12;
                            if (in.skip(bytes) != bytes) {
                                return;
                            }
                            int modSize = in.readInt();
                            for (int i = 0; i < modSize; i++) {
                                String modName = Util.readString(in);
                                short major = in.readShort();
                                short minor = in.readShort();
                                short build = in.readShort();
                                Version version = new Version(major, minor, build);

                                Mod mod = Datastore.getInstance().getMod(modName, version);
                                mods.add(mod);
                            }
                        } else if (zipEntryName.equals("preview.png")) {
                            // read screenshot
                            Image img = new Image(zip);
                            this.screenshot.set(img);
                        }
                        if (mods.size() > 0 && this.screenshot.get() != null) {
                            break;
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.path.set(path);
    }

    public Path getPath() {
        return path.get();
    }

    public ObjectProperty<Path> pathProperty() {
        return path;
    }

    public void setPath(Path path) {
        this.path.set(path);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Image getScreenshot() {
        return screenshot.get();
    }

    public ObjectProperty<Image> screenshotProperty() {
        return screenshot;
    }

    public void setScreenshot(Image screenshot) {
        this.screenshot.set(screenshot);
    }

    public ObservableList<Mod> getMods() {
        return mods;
    }
}
