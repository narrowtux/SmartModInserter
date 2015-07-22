package com.narrowtux.fmm;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by tux on 22.07.15.
 */
public class Util {
    public static String readString(LittleEndianDataInputStream objectInputStream) throws IOException {
        int size = objectInputStream.readInt();
        byte read[] = new byte[size];
        objectInputStream.read(read);
        return new String(read);
    }
}
