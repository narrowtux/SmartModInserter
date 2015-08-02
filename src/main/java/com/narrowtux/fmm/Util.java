package com.narrowtux.fmm;

import com.google.common.io.LittleEndianDataInputStream;
import com.narrowtux.fmm.gui.Controller;
import com.narrowtux.fmm.gui.ModpackListWindowController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;

import javax.measure.quantity.DataAmount;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;

public class Util {
    public static Unit<DataAmount> KILO_BYTES = SI.KILO(NonSI.BYTE);
    public static Unit<DataAmount> MEGA_BYTES = SI.MEGA(NonSI.BYTE);

    public static String readString(LittleEndianDataInputStream objectInputStream) throws IOException {
        int size = objectInputStream.readInt();
        byte read[] = new byte[size];
        objectInputStream.read(read);
        return new String(read);
    }

    public <T extends Controller> Node loadFXML(URL resource, T controller) throws IOException {
        return FXMLLoader.load(resource, null, new JavaFXBuilderFactory(), clazz -> controller);
    }
}
