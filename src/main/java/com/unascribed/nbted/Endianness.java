/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 - 2023 Una Thompson (unascribed)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.unascribed.nbted;

import io.github.steveice10.opennbt.NBTIO.LittleEndianDataInputStream;
import io.github.steveice10.opennbt.NBTIO.LittleEndianDataOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum Endianness {
    BIG,
    LITTLE,
    ZZAZZ;

    private static final byte[] EGG_NOISE = "ZZAZZAAZZAAZZZAAZZZAZAZZAZAZAZZAAZZAAZAZAZAZZAZAZAZAZAZAZZAAZZAAZZAAZZAAAZAZAZAAZZAAZZAAZAZZAAZZAAZZAZAZAZZAZZAZZAZZAZZZZZAAZAZAZAZAZAZAZ".getBytes(StandardCharsets.UTF_8);

    public DataInput wrap(InputStream in) {
        return switch (this) {
            case BIG -> new DataInputStream(in);
            case LITTLE -> new LittleEndianDataInputStream(in);
            case ZZAZZ -> new DataInputStream(new XORInputStream(in, EGG_NOISE));
            default -> throw new AssertionError("missing case for " + this);
        };
    }

    public DataOutput wrap(OutputStream out) {
        return switch (this) {
            case BIG -> new DataOutputStream(out);
            case LITTLE -> new LittleEndianDataOutputStream(out);
            case ZZAZZ -> new DataOutputStream(new XOROutputStream(out, EGG_NOISE));
            default -> throw new AssertionError("missing case for " + this);
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case BIG -> "Big (Java Edition)";
            case LITTLE -> "Little (Legacy Pocket Edition)";
            case ZZAZZ -> "ZZAZZAAZZ (ZZAAZZAAZZAAZZAAZZAAZZAAZZAAZZ)";
            default -> throw new AssertionError("missing case for " + this);
        };
    }

    private static class XORInputStream extends FilterInputStream {
        private final byte[] key;
        private int counter = 0;

        protected XORInputStream(InputStream in, byte[] key) {
            super(in);
            this.key = key;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b == -1) return -1;
            b = (byte) (b ^ key[counter]) & 0xFF;
            counter = (counter + 1) % key.length;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            len = in.read(b, off, len);
            for (int i = 0; i < len; i++) {
                b[off + i] = (byte) (b[off + i] ^ key[counter]);
                counter = (counter + 1) % key.length;
            }
            return len;
        }

    }

    private static class XOROutputStream extends FilterOutputStream {
        private final byte[] key;
        private int counter = 0;

        protected XOROutputStream(OutputStream out, byte[] key) {
            super(out);
            this.key = key;
        }

        @Override
        public void write(int b) throws IOException {
            out.write((byte) (b ^ key[counter]) & 0xFF);
            counter = (counter + 1) % key.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] copy = Arrays.copyOfRange(b, off, off + len);
            for (int i = 0; i < copy.length; i++) {
                copy[i] = (byte) (copy[i] ^ key[counter]);
                counter = (counter + 1) % key.length;
            }
            out.write(copy);
        }

    }

}
