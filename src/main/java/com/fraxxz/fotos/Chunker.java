package com.fraxxz.fotos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Chunker {
    public static List<byte[]> split(byte[] data) {
        List<byte[]> out = new ArrayList<>();
        for (int i = 0; i < data.length; i += FotosMod.CHUNK) {
            out.add(Arrays.copyOfRange(data, i, Math.min(data.length, i + FotosMod.CHUNK)));
        }
        return out;
    }
}
