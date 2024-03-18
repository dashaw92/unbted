/*
 * Copyright (C) 2013-2017 Steveice10, 2018 - 2023 Una Thompson (unascribed)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.steveice10.opennbt.tag.number;

import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTReader;
import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTWriter;
import io.github.steveice10.opennbt.tag.NBTTag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTFloat extends NBTNumber implements Comparable<NBTFloat> {
    private float value;

    public NBTFloat(String name) {
        this(name, 0);
    }

    public NBTFloat(String name, float value) {
        super(name);
        this.value = value;
    }

    @Override
    public Float numberValue() {
        return this.value;
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    @Override
    public long longValue() {
        return (long) this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    @Override
    public String stringValue() {
        return Float.toString(this.value);
    }

    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public void read(DataInput in) throws IOException {
        this.value = in.readFloat();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeFloat(this.value);
    }

    @Override
    public void destringify(StringifiedNBTReader in) throws IOException {
        String s = in.readNextSingleValueString();
        s = s.toLowerCase().substring(0, s.length() - 1);
        value = Float.parseFloat(s);
    }

    @Override
    public void stringify(StringifiedNBTWriter out, boolean linebreak, int depth) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        sb.append('f');
        out.append(sb.toString());
    }

    @Override
    public int compareTo(NBTFloat that) {
        return Float.compare(this.value, that.value);
    }

    @Override
    protected boolean equalsChecked(NBTTag that) {
        return this.value == ((NBTFloat) that).value;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }

    @Override
    public String toString() {
        return "NBTFloat[value=" + value + "]";
    }

}
