/*
 * Copyright (C) 2013-2017 Steveice10
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

package io.github.steveice10.opennbt.tag.builtin;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.steveice10.opennbt.NBTIO;

import java.util.Set;

/**
 * A compound tag containing other tags.
 */
public class CompoundTag extends Tag implements Iterable<Tag> {
    private Map<String, Tag> value;

    /**
     * Creates a tag with the specified name.
     *
     * @param name The name of the tag.
     */
    public CompoundTag(String name) {
        this(name, new LinkedHashMap<String, Tag>());
    }

    /**
     * Creates a tag with the specified name.
     *
     * @param name  The name of the tag.
     * @param value The value of the tag.
     */
    public CompoundTag(String name, Map<String, Tag> value) {
        super(name);
        this.value = new LinkedHashMap<String, Tag>(value);
    }

    @Override
    public Map<String, Tag> getValue() {
        return new LinkedHashMap<String, Tag>(this.value);
    }

    /**
     * Sets the value of this tag.
     *
     * @param value New value of this tag.
     */
    public void setValue(Map<String, Tag> value) {
        this.value = new LinkedHashMap<String, Tag>(value);
    }

    /**
     * Checks whether the compound tag is empty.
     *
     * @return Whether the compound tag is empty.
     */
    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    /**
     * Checks whether the compound tag contains a tag with the specified name.
     *
     * @param tagName Name of the tag to check for.
     * @return Whether the compound tag contains a tag with the specified name.
     */
    public boolean contains(String tagName) {
        return this.value.containsKey(tagName);
    }

    /**
     * Gets the tag with the specified name.
     *
     * @param <T>     Type of tag to get.
     * @param tagName Name of the tag.
     * @return The tag with the specified name.
     */
    public <T extends Tag> T get(String tagName) {
        return (T) this.value.get(tagName);
    }

    /**
     * Puts the tag into this compound tag.
     *
     * @param <T> Type of tag to put.
     * @param tag Tag to put into this compound tag.
     * @return The previous tag associated with its name, or null if there wasn't one.
     */
    public <T extends Tag> T put(T tag) {
        return (T) this.value.put(tag.getName(), tag);
    }

    /**
     * Removes a tag from this compound tag.
     *
     * @param <T>     Type of tag to remove.
     * @param tagName Name of the tag to remove.
     * @return The removed tag.
     */
    public <T extends Tag> T remove(String tagName) {
        return (T) this.value.remove(tagName);
    }

    /**
     * Gets a set of keys in this compound tag.
     *
     * @return The compound tag's key set.
     */
    public Set<String> keySet() {
        return this.value.keySet();
    }

    /**
     * Gets a collection of tags in this compound tag.
     *
     * @return This compound tag's tags.
     */
    public Collection<Tag> values() {
        return this.value.values();
    }

    /**
     * Gets the number of tags in this compound tag.
     *
     * @return This compound tag's size.
     */
    public int size() {
        return this.value.size();
    }

    /**
     * Clears all tags from this compound tag.
     */
    public void clear() {
        this.value.clear();
    }

    @Override
    public Iterator<Tag> iterator() {
        return this.values().iterator();
    }

    @Override
    public void read(DataInput in) throws IOException {
        List<Tag> tags = new ArrayList<Tag>();
        try {
            Tag tag;
            while((tag = NBTIO.readTag(in)) != null) {
                tags.add(tag);
            }
        } catch(EOFException e) {
            throw new IOException("Closing EndTag was not found!");
        }

        for(Tag tag : tags) {
            this.put(tag);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        for(Tag tag : this.value.values()) {
            NBTIO.writeTag(out, tag);
        }

        out.writeByte(0);
    }

    @Override
    public CompoundTag clone() {
        Map<String, Tag> newMap = new LinkedHashMap<String, Tag>();
        for(Entry<String, Tag> entry : this.value.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue().clone());
        }

        return new CompoundTag(this.getName(), newMap);
    }
}
