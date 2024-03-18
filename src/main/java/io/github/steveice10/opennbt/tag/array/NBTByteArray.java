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

package io.github.steveice10.opennbt.tag.array;

import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTReader;
import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTWriter;
import io.github.steveice10.opennbt.tag.NBTParent;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.support.NBTFakeByte;
import io.github.steveice10.opennbt.tag.number.NBTByte;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NBTByteArray extends NBTArray implements NBTParent {
    private List<Byte> value;

    public NBTByteArray(String name) {
        this(name, new byte[0]);
    }

    public NBTByteArray(String name, byte[] value) {
        super(name);
        this.value = arrToList(value);
    }

    private static List<Byte> arrToList(byte[] bytes) {
        var list = new ArrayList<Byte>(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            list.add(bytes[i]);
        }
        return list;
    }

    private static byte[] listToArr(List<Byte> bytes) {
        byte[] arr = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            arr[i] = bytes.get(i);
        }
        return arr;
    }

    public byte[] getValue() {
        return listToArr(this.value);
    }

    public void setValue(byte[] value) {
        if (value == null) return;
        this.value = arrToList(value);
    }

    public byte getValue(int index) {
        return this.value.get(index);
    }

    public void setValue(int index, byte value) {
        this.value.set(index, value);
    }

    @Override
    public String stringValue() {
        return Arrays.toString(listToArr(value));
    }

    @Override
    public void read(DataInput in) throws IOException {
        byte[] arr = new byte[in.readInt()];
        in.readFully(arr);
        this.value = arrToList(arr);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.value.size());
        out.write(listToArr(this.value));
    }

    @Override
    public void destringify(StringifiedNBTReader in) throws IOException {
        String s = in.readUntil(true, ']');
        String[] valueStrings = s.substring(s.indexOf(';') + 1, s.length() - 1).replaceAll(" ", "").split(",");
        byte[] newVal = new byte[valueStrings.length];
        for (int i = 0; i < value.size(); i++) {
            newVal[i] = Byte.parseByte(valueStrings[i]);
        }

        value = arrToList(newVal);
    }

    @Override
    public void stringify(StringifiedNBTWriter out, boolean linebreak, int depth) throws IOException {
        StringBuilder sb = new StringBuilder("[B; ");
        for (byte b : value) {
            sb.append(b);
            sb.append(',');
            sb.append(' ');
        }
        sb.setLength(sb.length() - 2);
        sb.append(']');
        out.append(sb.toString());
    }

    @Override
    protected boolean equalsChecked(NBTTag that) {
        return this.value.equals(((NBTByteArray) that).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return "NBTByteArray" + Arrays.toString(listToArr(this.value));
    }

    @Override
    public Class<? extends NBTTag> getElementType() {
        return NBTByte.class;
    }

    @Override
    public Iterator<NBTTag> iterator() {
        return new Iterator<NBTTag>() {

            private int idx = -1;

            @Override
            public boolean hasNext() {
                return idx < value.size();
            }

            @Override
            public NBTTag next() {
                idx++;
                return new NBTFakeByte(NBTByteArray.this, idx);
            }
        };
    }

    @Override
    public NBTFakeByte get(int idx) {
        if (idx < 0 || idx >= value.size()) throw new ArrayIndexOutOfBoundsException(idx);
        return new NBTFakeByte(this, idx);
    }

    @Override
    public boolean add(int idx, NBTTag tag) {
        if (tag instanceof NBTByte) {
            value.add(idx, ((NBTByte) tag).byteValue());
            return true;
        }
        return false;
    }

    @Override
    public boolean add(NBTTag tag) {
        if (tag instanceof NBTByte) {
            value.add(((NBTByte) tag).byteValue());
            return true;
        }
        return false;
    }

    @Override
    public NBTTag set(int idx, NBTTag tag) {
        if (tag instanceof NBTByte) {
            byte orig = value.get(idx);
            value.set(idx, ((NBTByte) tag).byteValue());
            return new NBTByte("", orig);
        }
        throw new ClassCastException(tag.getClass().getSimpleName() + " is not NBTByte");
    }

    @Override
    public boolean remove(NBTTag tag) {
        if (tag instanceof NBTFakeByte) {
            NBTFakeByte nfb = (NBTFakeByte) tag;
            if (nfb.getParent() == this) {
                value.remove(nfb.getIndex());
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return value.size();
    }

    @Override
    public boolean isEmpty() {
        return value.size() == 0;
    }

    @Override
    public void clear() {
        value = new ArrayList<>();
    }

}
