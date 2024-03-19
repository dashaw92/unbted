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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiCode;
import com.unascribed.miniansi.AnsiStream;
import io.github.steveice10.opennbt.NBTRegistry;
import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.NBTIntArray;
import io.github.steveice10.opennbt.tag.array.NBTLongArray;
import io.github.steveice10.opennbt.tag.number.NBTByte;
import io.github.steveice10.opennbt.tag.number.NBTLong;
import io.github.steveice10.opennbt.tag.number.NBTNumber;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TagPrinter {

    private static final Escaper escaper = new Escaper()
            .addEscape('\u0000', "\\0")
            .addEscape('\u0001', "\\x01")
            .addEscape('\u0002', "\\x02")
            .addEscape('\u0003', "\\x03")
            .addEscape('\u0004', "\\x04")
            .addEscape('\u0005', "\\x05")
            .addEscape('\u0006', "\\x06")
            .addEscape('\u0007', "\\a")
            .addEscape('\u0008', "\\b")
            .addEscape('\u0009', "\\t")
            .addEscape('\n', "\\n")
            .addEscape('\u000B', "\\v")
            .addEscape('\u000C', "\\f")
            .addEscape('\r', "\\r")
            .addEscape('\u000E', "\\x0E")
            .addEscape('\u000F', "\\x0F")
            .addEscape('\u0010', "\\x10")
            .addEscape('\u0011', "\\x11")
            .addEscape('\u0012', "\\x12")
            .addEscape('\u0013', "\\x13")
            .addEscape('\u0014', "\\x14")
            .addEscape('\u0015', "\\x15")
            .addEscape('\u0016', "\\x16")
            .addEscape('\u0017', "\\x17")
            .addEscape('\u0018', "\\x18")
            .addEscape('\u0019', "\\x19")
            .addEscape('\u001A', "\\x1A")
            .addEscape('\u001B', "\\e")
            .addEscape('\u001C', "\\x1C")
            .addEscape('\u001D', "\\x1D")
            .addEscape('\u001E', "\\x1E")
            .addEscape('\u001F', "\\x1F")
            .addEscape('\u007F', "\\x7F")
            .addEscape('\\', "\\\\");
    private static final List<String> knownBooleanNames = List.of(
            "hardcore"
    );
    private static final List<String> likelyBooleanPrefixes = List.of(
            "has", "is", "seen", "should", "on", "flag", "bool", "boolean"
    );
    private static final List<String> likelyBooleanSuffixes = List.of(
            "ing", "locked", "flag", "boolean", "bool"
    );
    private final AnsiStream aout;
    private final Gson gson = new Gson();

    public TagPrinter(PrintStream out) {
        this(new AnsiStream(out));
    }

    public TagPrinter(AnsiStream out) {
        this.aout = out;
    }

    public void printTag(NBTTag tag) {
        printTag(tag, "");
    }

    public void printTag(NBTTag tag, String prefix) {
        printTag(tag, prefix, NBTEd.INFER);
    }

    public void printTag(NBTTag tag, String prefix, boolean infer) {
        printTag(tag, prefix, infer, RecurseMode.FULL);
    }

    public void printTag(NBTTag tag, String prefix, boolean infer, RecurseMode recurse) {
        printTag(tag, prefix, infer, recurse, true);
    }

    public void printTag(NBTTag tag, String prefix, boolean infer, RecurseMode recurse, boolean values) {
        if (tag == null) {
            return;
        }
        switch (tag) {
            case NBTCompound ct -> {
                if (recurse.shouldPrintRoot()) {
                    aout.print(prefix);
                    aout.print("compound ", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
                    printName(tag, tag.getName(), false);
                }
                if (values || (recurse.shouldPrintChildren() && !recurse.shouldPrintRoot())) {
                    if (recurse.shouldPrintChildren()) {
                        if (ct.isEmpty()) {
                            if (recurse.shouldPrintRoot()) aout.println(" {}", AnsiCode.RESET);
                        } else {
                            if (recurse.shouldPrintRoot()) aout.println(" {", AnsiCode.RESET);
                            String childPrefix = recurse.shouldPrintRoot() ? prefix + "  " : prefix;
                            for (NBTTag t : ct) {
                                if (infer) {
                                    if (t.getName().endsWith("Most") && ct.contains(t.getName().replaceFirst("Most$", "Least"))) {
                                        NBTTag least = ct.get(t.getName().replaceFirst("Most$", "Least"));
                                        if (t instanceof NBTLong && least instanceof NBTLong) {
                                            UUID u = new UUID(((NBTLong) t).longValue(), ((NBTLong) least).longValue());
                                            printBasic(tag, u.toString(), t.getName().replaceFirst("Most$", ""), "~uuid", AnsiCode.FG_YELLOW, childPrefix, values);
                                            continue;
                                        }
                                    }
                                    if (t.getName().endsWith("Least") && ct.contains(t.getName().replaceFirst("Least$", "Most"))) {
                                        NBTTag most = ct.get(t.getName().replaceFirst("Least$", "Most"));
                                        if (most instanceof NBTLong && t instanceof NBTLong) {
                                            continue;
                                        }
                                    }
                                }
                                printTag(t, childPrefix, infer, recurse.degradeForCompound(), values);
                            }
                            if (recurse.shouldPrintRoot()) {
                                aout.print(prefix);
                                aout.println("}");
                            }
                        }
                    } else if (recurse.shouldPrintRoot()) {
                        aout.print(" (", AnsiCode.RESET);
                        aout.print(ct.size());
                        aout.print(" child");
                        if (ct.size() != 1) {
                            aout.print("ren");
                        }
                        aout.println(")");
                    }
                } else {
                    aout.println(AnsiCode.RESET);
                }
            }
            case NBTList lt -> {
                if (lt.isEmpty()) {
                    if (recurse.shouldPrintRoot()) {
                        aout.print(prefix);
                        aout.print("list ", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
                        printName(tag, tag.getName(), false);
                        if (values) {
                            if (recurse.shouldPrintChildren()) {
                                aout.println(" []", AnsiCode.RESET);
                            } else {
                                aout.println(" (0 children)", AnsiCode.RESET);
                            }
                        } else {
                            aout.println(AnsiCode.RESET);
                        }
                    }
                } else {
                    boolean forgeRegistry = true;
                    if (recurse.shouldPrintChildren() && values || (recurse.shouldPrintChildren() && !recurse.shouldPrintRoot())) {
                        Class<?> registryType = null;
                        if (infer) {
                            for (NBTTag t : lt) {
                                if (t instanceof NBTCompound ct) {
                                    if (ct.size() == 2 && ct.contains("K") && ct.contains("V") && ct.get("V") instanceof Comparable) {
                                        if (registryType == null) {
                                            registryType = ct.get("V").getClass();
                                            continue;
                                        } else if (registryType == ct.get("V").getClass()) {
                                            continue;
                                        }
                                    }
                                }
                                forgeRegistry = false;
                                break;
                            }
                        } else {
                            forgeRegistry = false;
                        }
                    } else {
                        forgeRegistry = false;
                    }
                    if (forgeRegistry) {
                        if (recurse.shouldPrintRoot()) {
                            aout.print(prefix);
                            aout.print("~registry ", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
                            printName(tag, tag.getName(), false);
                            aout.println(" [", AnsiCode.RESET);
                        }
                        // these assumptions are safe due to the checks above that set the forgeRegistry flag
                        List<NBTCompound> copy = StreamSupport.stream(lt.spliterator(), false)
                                .map(_tag -> (NBTCompound) _tag).sorted(new Comparator<NBTCompound>() {
                                    @Override
                                    @SuppressWarnings({"unchecked", "rawtypes"})
                                    public int compare(NBTCompound a, NBTCompound b) {
                                        return ((Comparable) a.get("V")).compareTo(b.get("V"));
                                    }
                                }).collect(Collectors.toCollection(ArrayList::new));

                        int indexLength = Integer.toString(lt.size() - 1).length();
                        for (NBTCompound t : copy) {
                            if (t.size() == 2 && t.contains("K") && t.contains("V")) {
                                aout.print(prefix);
                                if (recurse.shouldPrintRoot()) aout.print("  ");
                                String str = Integer.toString(lt.indexOf(t));
                                for (int i = str.length(); i < indexLength; i++) {
                                    aout.print(" ");
                                }
                                aout.print(str, AnsiCode.RESET);
                                aout.print(": ");
                                printName(tag, t.get("K").stringValue(), true);
                                printVal(t.get("V").stringValue());
                                continue;
                            }
                        }
                        if (recurse.shouldPrintRoot()) {
                            aout.print(prefix);
                            aout.println("]");
                        }
                    } else {
                        if (recurse.shouldPrintRoot()) {
                            aout.print(prefix);
                            aout.print("list ", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
                            printName(tag, tag.getName(), false);
                        }
                        if (values || (recurse.shouldPrintChildren() && !recurse.shouldPrintRoot())) {
                            if (recurse.shouldPrintChildren()) {
                                if (recurse.shouldPrintRoot()) aout.println(" [", AnsiCode.RESET);
                                for (NBTTag t : lt) {
                                    printTag(t, recurse.shouldPrintRoot() ? prefix + "  " : prefix, infer, recurse.degradeForList(), values);
                                }
                                if (recurse.shouldPrintRoot()) {
                                    aout.print(prefix);
                                    aout.println("]");
                                }
                            } else if (recurse.shouldPrintRoot()) {
                                aout.print(" (", AnsiCode.RESET);
                                aout.print(lt.size());
                                aout.print(" child");
                                if (lt.size() != 1) {
                                    aout.print("ren");
                                }
                                aout.println(")");
                            }
                        } else {
                            aout.println(AnsiCode.RESET);
                        }
                    }
                }
            }
            case NBTByte nbtByte -> {
                if (infer && nbtByte.byteValue() == 0 || nbtByte.byteValue() == 1) {
                    String lowerName = tag.getName().toLowerCase(Locale.ROOT);
                    boolean maybeBoolean = false;
                    if (knownBooleanNames.contains(lowerName)) {
                        maybeBoolean = true;
                    } else {
                        for (String s : likelyBooleanPrefixes) {
                            if (lowerName.startsWith(s)) {
                                maybeBoolean = true;
                                break;
                            }
                        }
                        if (!maybeBoolean) {
                            for (String s : likelyBooleanSuffixes) {
                                if (lowerName.endsWith(s)) {
                                    maybeBoolean = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (maybeBoolean) {
                        printBasic(tag, Boolean.toString(((NBTByte) tag).booleanValue()), tag.getName(), "~bool", AnsiCode.FG_YELLOW, prefix, values);
                        return;
                    }
                }
                printBasic(tag, tag.stringValue(), tag.getName(), "byte", AnsiCode.FG_YELLOW, prefix, values);
            }
            case NBTNumber nbtNumber ->
                    printBasic(tag, tag.stringValue(), tag.getName(), NBTRegistry.typeNameFromClass(tag.getClass()), AnsiCode.FG_YELLOW, prefix, values);
            case NBTString nbtString -> {
                if (infer) {
                    String str = tag.stringValue();
                    if (str.startsWith("{") || str.startsWith("[")) {
                        try {
                            JsonElement je = gson.fromJson(str, JsonElement.class);
                            if (values) {
                                StringWriter sw = new StringWriter();
                                JsonWriter jw = new JsonWriter(sw);
                                jw.setIndent("  ");
                                jw.setLenient(true);
                                gson.toJson(je, jw);
                                String jstr = sw.toString()
                                        .replaceAll("[]\\[]", AnsiCode.RESET + "$0")
                                        .replace("\n", "\n" + prefix + "  " + AnsiCode.FG_BLUE_INTENSE)
                                        .replace(":", AnsiCode.RESET + ":" + AnsiCode.FG_GREEN)
                                        .replace(",", AnsiCode.RESET + ",")
                                        .replace("{", AnsiCode.RESET + "{")
                                        .replace("}", AnsiCode.RESET + "}");
                                printBasic(tag, recurse.shouldPrintChildren() ? jstr : "...", tag.getName(), "~json", AnsiCode.FG_RED_INTENSE, prefix, true);
                            } else {
                                printBasic(tag, null, tag.getName(), "~json", AnsiCode.FG_RED_INTENSE, prefix, false);
                            }
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                }
                printBasic(tag, tag.stringValue(), tag.getName(), "string", AnsiCode.FG_RED, prefix, values);
            }
            case NBTByteArray nbtTags -> {
                if (infer && nbtTags.size() > 32) {
                    printBasic(tag, Base64.getEncoder().encodeToString(((NBTByteArray) tag).getValue()), tag.getName(), "~base64", AnsiCode.FG_YELLOW_INTENSE, prefix, values);
                } else {
                    printBasic(tag, colorizeArray(tag.stringValue()), tag.getName(), "byte[]", AnsiCode.FG_YELLOW_INTENSE, prefix, values);
                }
            }
            case NBTIntArray arr -> {
                if (infer && arr.size() == 4) {
                    printBasic(tag, UUIDs.fromIntArray(arr.getValue()).toString(), tag.getName(), "~uuid", AnsiCode.FG_YELLOW_INTENSE, prefix, values);
                } else {
                    printBasic(tag, colorizeArray(tag.stringValue()), tag.getName(), "int[]", AnsiCode.FG_YELLOW_INTENSE, prefix, values);
                }
            }
            case NBTLongArray nbtTags ->
                    printBasic(tag, colorizeArray(tag.stringValue()), tag.getName(), "long[]", AnsiCode.FG_YELLOW_INTENSE, prefix, values);
            default -> {
            }
        }
    }

    private String colorizeArray(String str) {
        return str
                .replaceAll("[]\\[]", AnsiCode.RESET + "$0" + AnsiCode.FG_GREEN)
                .replace(",", AnsiCode.RESET + "," + AnsiCode.FG_GREEN);
    }

    public void printBasic(NBTTag tag, String val, String name, String type, AnsiCode color, String prefix, boolean values) {
        aout.print(prefix);
        aout.print(type, color);
        aout.print(" ");
        printName(tag, name, values);
        if (values) printVal(val);
    }

    public void printVal(String val) {
        aout.print(AnsiCode.FG_GREEN);
        aout.print(val);
        aout.println(AnsiCode.RESET);
    }

    public void printName(NBTTag tag, String name, boolean equals) {
        if (name != null && !name.isEmpty()) {
            aout.print("\"", AnsiCode.FG_BLUE_INTENSE);
            aout.print(escaper.escape(name));
            aout.print("\"");
            if (equals) {
                aout.print(" = ", AnsiCode.RESET);
            }
        } else {
            if (tag.getParent() instanceof NBTList) {
                String str = Integer.toString(((NBTList) tag.getParent()).indexOf(tag));
                int maxLen = Integer.toString(((NBTList) tag.getParent()).size() - 1).length();
                for (int i = str.length(); i < maxLen; i++) {
                    aout.print(" ");
                }
                aout.print(str, AnsiCode.RESET);
                if (equals) {
                    aout.print(" = ");
                }
            } else if (equals) {
                aout.print(" ");
            }
        }
    }

    public enum RecurseMode {
        /**
         * Only print the passed tag, and a summary of its children if it has
         * them.
         */
        NONE,
        /**
         * Print the passed tag and its immediate children.
         */
        IMMEDIATE_CHILDREN,
        /**
         * Print the passed tag's immediate children.
         */
        IMMEDIATE_CHILDREN_ONLY,
        /**
         * Print the passed tag and all of its descendants.
         */
        FULL;

        public boolean shouldPrintRoot() {
            return this != IMMEDIATE_CHILDREN_ONLY;
        }

        public boolean shouldPrintChildren() {
            return this != NONE;
        }

        public RecurseMode degradeForCompound() {
            return switch (this) {
                case NONE -> NONE;
                case IMMEDIATE_CHILDREN -> NONE;
                case IMMEDIATE_CHILDREN_ONLY -> NONE;
                case FULL -> FULL;
                default -> throw new AssertionError("missing case for " + this);
            };
        }

        public RecurseMode degradeForList() {
            return switch (this) {
                case NONE -> NONE;
                case IMMEDIATE_CHILDREN -> NONE;
                case IMMEDIATE_CHILDREN_ONLY -> IMMEDIATE_CHILDREN;
                case FULL -> FULL;
                default -> throw new AssertionError("missing case for " + this);
            };
        }
    }

}
