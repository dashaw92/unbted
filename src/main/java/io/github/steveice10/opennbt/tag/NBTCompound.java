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

package io.github.steveice10.opennbt.tag;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTReader;
import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public class NBTCompound extends NBTTag implements NBTParent {
	private final Map<String, NBTTag> map = new LinkedHashMap<>();

	public NBTCompound(String name) {
		super(name);
	}

	public NBTCompound(String name, Map<String, NBTTag> value) {
		super(name);
		for (NBTTag tag : value.values()) {
			put(tag);
		}
	}

	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public boolean contains(String tagName) {
		return this.map.containsKey(tagName);
	}

	public <T extends NBTTag> T get(String tagName) {
		return (T) this.map.get(tagName);
	}

	public <T extends NBTTag> T put(T tag) {
		T t = (T) this.map.put(tag.getName(), tag);
		tag.setParent(this);
		if (t != null) {
			t.setParent(null);
		}
		return t;
	}

	public <T extends NBTTag> T remove(String tagName) {
		T t = (T) this.map.remove(tagName);
		if (t != null) {
			t.setParent(null);
		}
		return t;
	}

	@Override
	public boolean remove(NBTTag tag) {
		if (this.map.remove(tag.getName(), tag)) {
			tag.setParent(null);
			return true;
		}
		return false;
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

	public Collection<NBTTag> values() {
		return Collections.unmodifiableCollection(this.map.values());
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public void clear() {
		for (NBTTag tag : map.values()) {
			tag.setParent(null);
		}
		this.map.clear();
	}

	@Override
	public Iterator<NBTTag> iterator() {
		return this.values().iterator();
	}

	@Override
	public String stringValue() {
        List<String> inner = values().stream().map(t -> t.getName() + ": " + t.stringValue()).toList();
        return "{" + String.join(", ", inner) + "}";
	}

	@Override
	public void read(DataInput in) throws IOException {
		clear();
		try {
			while (true) {
				NBTTag tag = NBTIO.readTag(in);
				if (tag == null) break;
				put(tag);
			}
		} catch (EOFException e) {
			throw new IOException("Compound end marker not found", e);
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		for (NBTTag tag : this.map.values()) {
			NBTIO.writeTag(out, tag);
		}
		out.writeByte(0);
	}

	@Override
	public void destringify(StringifiedNBTReader in) throws IOException {
		in.readSkipWhitespace();
		while (true) {
			String tagName = "";
			if ((tagName += in.readSkipWhitespace()).equals("\"")) {
				tagName = in.readUntil(false, '"');
				in.read();
			}
			tagName += in.readUntil(false, ':');
			in.read();

			put(in.readNextTag(tagName));

			char endChar = in.readSkipWhitespace();
			if (endChar == ',')
				continue;
			if (endChar == '}')
				break;
		}
	}

	@Override
	public void stringify(StringifiedNBTWriter out, boolean linebreak, int depth) throws IOException {
		out.append('{');

		boolean first = true;
		for (NBTTag t : map.values()) {
			if (first) {
				first = false;
			} else {
				out.append(',');
				if (!linebreak) {
					out.append(' ');
				}
			}
			out.writeTag(t, linebreak, depth + 1);
		}

		if (linebreak) {
			out.append('\n');
			out.indent(depth);
		}
		out.append('}');
	}

	@Override
	protected boolean equalsChecked(NBTTag that) {
		return Objects.equals(this.map, ((NBTCompound)that).map);
	}

	@Override
	public int hashCode() {
		return this.map.hashCode();
	}

	@Override
	public String toString() {
		return "NBTCompound"+map+"";
	}

}
