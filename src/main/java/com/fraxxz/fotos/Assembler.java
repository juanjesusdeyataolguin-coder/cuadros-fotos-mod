package com.fraxxz.fotos;

import java.io.ByteArrayOutputStream;

public final class Assembler {
    public final byte[][] parts;
    private int got;

    public Assembler(int n) { parts = new byte[n][]; }

    /** Devuelve todos los bytes cuando ya llegaron todas las partes; si no, null. */
    public byte[] add(int i, byte[] d) {
        if (parts[i] == null) { parts[i] = d; got++; }
        if (got < parts.length) return null;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) o.write(p, 0, p.length);
        return o.toByteArray();
    }
}
