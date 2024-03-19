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

import io.airlift.compress.zstd.ZstdInputStream;
import io.airlift.compress.zstd.ZstdOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public enum Compression {
    NONE("None"),
    DEFLATE("Deflate"),
    GZIP("GZip"),
    ZSTD("ZStandard"),
    ;
    private final String name;

    Compression(String name) {
        this.name = name;
    }

    public InputStream wrap(InputStream is) throws IOException {
        if (is == null) return null;
        return switch (this) {
            case NONE -> is;
            case DEFLATE -> new InflaterInputStream(is);
            case GZIP -> new GZIPInputStream(is);
            case ZSTD -> new ZstdInputStream(is);
            default -> throw new AssertionError("missing case for " + this);
        };
    }

    public OutputStream wrap(OutputStream os) throws IOException {
        if (os == null) return null;
        return switch (this) {
            case NONE -> os;
            case DEFLATE -> new DeflaterOutputStream(os);
            case GZIP -> new GZIPOutputStream(os);
            case ZSTD -> new ZstdOutputStream(os);
            default -> throw new AssertionError("missing case for " + this);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
