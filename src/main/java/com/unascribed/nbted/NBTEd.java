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

import com.google.gson.*;
import com.unascribed.miniansi.AnsiStream;
import io.github.steveice10.opennbt.NBTRegistry;
import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.NBTIntArray;
import io.github.steveice10.opennbt.tag.array.NBTLongArray;
import io.github.steveice10.opennbt.tag.number.*;
import org.fusesource.jansi.AnsiConsole;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class NBTEd implements Runnable {
    public static final String VERSION;
    public static final AnsiStream aout = new AnsiStream(System.out);
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private static final Map<Class<? extends Throwable>, String> commonExceptions = Map.of(
            IndexOutOfBoundsException.class, "index out-of-bounds",
            AssertionError.class, "assertion",
            IllegalArgumentException.class, "illegal argument",
            IllegalStateException.class, "illegal state",
            NullPointerException.class, "null pointer",
            EOFException.class, "end-of-file",
            IOException.class, "IO",
            IOError.class, "IO",
            UncheckedIOException.class, "IO"
    );

    private static final DateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    @CommandLine.Option(
            names = {"--verbose", "--debug", "-d", "-v"},
            description = "Print more information and stacktraces.",
            defaultValue = "true"
    )
    public boolean VERBOSE;

    @CommandLine.Option(
            names = { "-c", "--compression" },
            description = "Assume the given compression method, rather than autodetecting. Valid methods are None, Deflate, and GZip. (case-insensitive)"
    )
    private Compression COMPRESSION_MODE;

    @CommandLine.ArgGroup
    private ConversionArgument JSON_MODE;

    private static final class ConversionArgument {
        @CommandLine.Option(
                names = { "-j", "--json" },
                description =
                        "Print out well-formed JSON instead of a human-readable " +
                        "colored format. The keys in the JSON match those in the NBT 1:1. " +
                        "Cannot be converted back to NBT; type information is lost in the " +
                        "conversion. (incompatible with -J and -N)"
        )
        boolean basic;
        @CommandLine.Option(
                names = { "-J", "--roundtrip-json" },
                description =
                        "Print out well-formed JSON where the keys are prefixed with the " +
                        "original NBT type. Can be converted back into NBT by invoking " +
                        "unbted with the JSON file as an argument. Can also be generated " +
                        "by the save command in an unbted session. (incompatible with -j and -N)"
        )
        boolean roundtrip;
        @CommandLine.Option(
                names = { "-N", "--convert-nbt" },
                description =
                        "Convert the given roundtrip JSON file to a binary NBT file, given " +
                        "as a second argument. The --endian and --compression options will " +
                        "be respected. (incompatible with -j and -J)"
        )
        boolean convert_nbt;

        JsonMode getMode() {
            if(basic) return JsonMode.BASIC;
            if(roundtrip) return JsonMode.ROUNDTRIP;

            //convert_nbt is handled separately from these.
            return JsonMode.NONE;
        }
    }

    @CommandLine.ArgGroup
    private PrintArgument PRINT_MODE;

    private static final class PrintArgument {
        @CommandLine.Option(
                names = {"-p", "--print"},
                description = "Exit after printing file contents. (incompatible with -n)"
        )
        boolean print;
        @CommandLine.Option(
                names = {"-n", "--no-print"},
                description = "Don't print out initial file contents, just edit. (incompatible with -p)"
        )
        boolean no_print;

        public enum PrintMode {
            DEFAULT,
            PRINT,
            NO_PRINT,
        }

        public PrintMode getPrintMode() {
            if(print) return PrintMode.PRINT;
            if(no_print) return PrintMode.NO_PRINT;
            return PrintMode.DEFAULT;
        }
    }

    @CommandLine.ArgGroup
    private EndianArgument ENDIAN_MODE;

    private static final class EndianArgument {
        @CommandLine.Option(
                names = {"--endian"},
                description =
                        "Assume the input is the given endianness, instead of attempting " +
                        "to detect it. Historically, Pocket Edition had little-endian " +
                        "NBT files as part of its save format, but this was replaced " +
                        "with a custom LevelDB format in 0.9.0. " +
                        "Tip: If you're attempting to read one of these files, they " +
                        "contain extra metadata at the start of the file. I wasn't able " +
                        "to obtain an example file to test with, so I did not add " +
                        "support for stripping these headers. You may attempt to strip " +
                        "them manually using dd - the entities.dat is supposed to have " +
                        "12 bytes of junk, and the level.dat is supposed to have 8. " +
                        "e.g. dd if=entities.dat of=entities.lnbt skip=12 iflag=skip_bytes " +
                        "Valid endiannesses are Big and Little. (case-insensitive)"
        )
        Endianness explicit;
        @CommandLine.Option(
                names = {"--little-endian"},
                description = "Alias for --endian=little"
        )
        boolean little_endian;
        @CommandLine.Option(
                names = {"--big-endian"},
                description = "Alias for --endian=big"
        )
        boolean big_endian;

        public Endianness getEndianess() {
            if(explicit != null) return explicit;
            if(little_endian) return Endianness.LITTLE;

            return Endianness.BIG;
        }
    }

    @CommandLine.Option(
            names = { "-r", "--raw" },
            description = "Don't infer types.",
            defaultValue = "true"
    )
    public boolean INFER;

    @CommandLine.Option(
            names = {"-V", "--version"},
            description = "Print the version of unbted and exit.",
            versionHelp = true
    )
    private boolean printVersion;

    @CommandLine.Option(
            names = {"-?", "-h", "--help"},
            description = "Print this help.",
            usageHelp = true
    )
    private boolean printHelp;
    public static Terminal terminal;

    static {
        String ver = null;
        try (var br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("version.txt")))) {
            ver = br.readLine();
        } catch (IOException ignored) {
        }

        VERSION = ver == null ? "?.?" : ver;
    }

    public void log(String msg, Object... args) {
        if (VERBOSE) {
            Throwable t = null;
            if (args.length > 0 && args[args.length - 1] instanceof Throwable throwable) {
                t = throwable;
                args = Arrays.copyOfRange(args, 0, args.length - 1);
            }
            System.err.print("unbted: ");
            System.err.print(fmt.format(new Date()));
            System.err.print(" ");
            System.err.printf(msg.replace("%", "%%").replace("{}", "%s"), args);
            System.err.println();
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        CommandLine unbted = new CommandLine(new NBTEd());
        try {
            unbted.parseArgs(args);
        } catch(CommandLine.ParameterException ex) {
            System.err.println("unbted: " + ex.getMessage());
            return;
        }

        if(unbted.isUsageHelpRequested()) {
            initializeTerminal();
            printHelp();
            return;
        }

        if(unbted.isVersionHelpRequested()) {
            System.err.println("Una's NBT Editor v" + VERSION);
            System.err.println("Copyright (C) 2018 - 2023 Una Thompson (unascribed)");
            System.err.println("License GPLv3+: GNU GPL version 3 or later <https://gnu.org/licenses/gpl.html>.");
            System.err.println("This is free software: you are free to change and redistribute it.");
            System.err.println("There is NO WARRANTY, to the extent permitted by law.");
            return;
        }

        NBTEd inst = unbted.getCommand();

        UncaughtExceptionHandler ueh = (t, e) -> {
            if (inst.VERBOSE) {
                e.printStackTrace();
            } else if ("main".equals(t.getName())) {
                String str = "";
                for (Map.Entry<Class<? extends Throwable>, String> en : commonExceptions.entrySet()) {
                    if (en.getKey().isAssignableFrom(e.getClass())) {
                        str = en.getValue() + " ";
                        break;
                    }
                }
                System.err.println("unbted: An unexpected " + str + "error occurred" + (e.getMessage() == null ? "." : ":"));
                if (e.getMessage() != null) {
                    System.err.println("unbted:     " + e.getMessage());
                }
                System.err.println("unbted: (run with --verbose for more information)");
                System.exit(3);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(ueh);
        Thread.currentThread().setUncaughtExceptionHandler(ueh);

        inst.run();

//        NonOptionArgumentSpec<String> nonoption = parser.nonOptions().ofType(String.class);
//


//

//        List<String> nonoptions = set.valuesOf(nonoption);
//        if (set.has("convert-nbt")) {
//            if (nonoptions.size() > 2) {
//                System.err.println("unbted: Too many arguments - only two arguments, the input NBT JSON and output NBT files, may be specified");
//                System.exit(1);
//                return;
//            }
//        } else if (nonoptions.size() > 1) {
//            System.err.println("unbted: Too many arguments - only one argument, the input file, may be specified");
//            System.exit(1);
//            return;
//        }
//        File sourceFile;
//        ExceptableSupplier<InputStream, IOException> inSupplier;
//        if (nonoptions.isEmpty()) {
//            sourceFile = null;
//            inSupplier = null;
//        } else {
//            String in = nonoptions.getFirst();
//            if ("-".equals(in)) {
//                byte[] bys = System.in.readAllBytes();
//                inSupplier = () -> new ByteArrayInputStream(bys);
//                sourceFile = FileInfo.STDIN;
//                log("Reading from stdin");
//            } else {
//                File f = new File(in);
//                inSupplier = () -> new FileInputStream(f);
//                sourceFile = f;
//                log("Reading from file {}", f);
//            }
//        }
//
//        if (set.has("convert-nbt")) {
//            if (nonoptions.size() < 2) {
//                System.err.println("unbted: Not enough arguments - need input NBT JSON file and output NBT file");
//                System.exit(1);
//                return;
//            }
//            Compression compression = set.valueOf(compressionOpt);
//            if (compression == null) {
//                System.err.println("unbted: A compression method must be specified for conversion from NBT JSON");
//                System.exit(1);
//                return;
//            }
//            Endianness endianness = Endianness.BIG;
//            if (set.has(endiannessOpt)) {
//                endianness = set.valueOf(endiannessOpt);
//            } else if (set.has("little-endian")) {
//                endianness = Endianness.LITTLE;
//            } else if (set.has("big-endian")) {
//                endianness = Endianness.BIG;
//            }
//            String out = nonoptions.get(1);
//            ExceptableSupplier<OutputStream, IOException> outSupplier;
//            if ("-".equals(out)) {
//                outSupplier = () -> System.out;
//                log("Writing to stdout");
//            } else {
//                File f = new File(out);
//                outSupplier = () -> new FileOutputStream(f);
//                log("Writing to file {}", f);
//            }
//            try {
//                NBTTag tag = loadJson(inSupplier.get());
//                try (OutputStream os = compression.wrap(outSupplier.get())) {
//                    NBTIO.writeTag(endianness.wrap(os), tag);
//                } catch (Exception e) {
//                    log("Error occurred while writing", e);
//                    System.err.println("unbted: Failed to save " + (sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
//                    System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
//                    System.exit(2);
//                    return;
//                }
//            } catch (Exception e) {
//                log("Exception while trying to load NBT file", e);
//                System.err.println("unbted: Failed to load " + (sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
//                System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
//                System.exit(2);
//                return;
//            }
//            return;
//        }
//
//        if (set.has("json")) {
//            JSON_MODE = JsonMode.BASIC;
//        } else if (set.has("roundtrip-json")) {
//            JSON_MODE = JsonMode.ROUNDTRIP;
//        }
//        if (set.has("raw")) {
//            INFER = false;
//        }
//        Compression compressionMethod = set.valueOf(compressionOpt);
//        Compression detectedCompressionMethod = null;
//        if (inSupplier != null) {
//            try (InputStream is = inSupplier.get()) {
//                int magic8 = is.read() & 0xff;
//                int magic16 = magic8 | ((is.read() << 8) & 0xff00);
//                if (magic16 == GZIPInputStream.GZIP_MAGIC) {
//                    detectedCompressionMethod = Compression.GZIP;
//                } else if (magic8 == 0x78) {
//                    detectedCompressionMethod = Compression.DEFLATE;
//                } else if (magic16 == 0xb528) {
//                    detectedCompressionMethod = Compression.ZSTD;
//                } else {
//                    detectedCompressionMethod = Compression.NONE;
//                }
//                log("Compression autodetected as {}", detectedCompressionMethod);
//            }
//        }
//        boolean compressionAutodetected;
//        if (compressionMethod == null) {
//            if (inSupplier != null) {
//                compressionMethod = detectedCompressionMethod;
//                log("Using autodetected compression method");
//                compressionAutodetected = true;
//            } else {
//                log("No compression specified for new buffer");
//                compressionAutodetected = false;
//            }
//        } else {
//            log("Compression set as {}", compressionMethod);
//            compressionAutodetected = false;
//        }
//        Endianness endianness = null;
//        if (set.has(endiannessOpt)) {
//            endianness = set.valueOf(endiannessOpt);
//        } else if (set.has("little-endian")) {
//            endianness = Endianness.LITTLE;
//        } else if (set.has("big-endian")) {
//            endianness = Endianness.BIG;
//        }
//        boolean isJson = false;
//        NBTTag tag = null;
//        if (inSupplier != null) {
//            try {
//                final Compression compressionMethodFinal = compressionMethod;
//                final ExceptableSupplier<InputStream, IOException> currentSupplier = inSupplier;
//                inSupplier = () -> compressionMethodFinal.wrap(currentSupplier.get());
//                try (PushbackInputStream is = new PushbackInputStream(inSupplier.get())) {
//                    int firstByte = is.read();
//                    is.unread(firstByte);
//                    if (firstByte == '{') {
//                        isJson = true;
//                        log("Detected JSON file");
//                        tag = loadJson(is);
//                    } else {
//                        log("Detected binary file");
//                        if (endianness != null) {
//                            tag = NBTIO.readTag(endianness.wrap(is));
//                        } else {
//                            try {
//                                tag = NBTIO.readTag(is, false);
//                                if (tag == null) throw new RuntimeException("Got null root tag");
//                                endianness = Endianness.BIG;
//                                log("Endianness autodetected as big-endian");
//                            } catch (Exception e) {
//                                try {
//                                    tag = NBTIO.readTag(is, true);
//                                    if (tag == null) throw new RuntimeException("Got null root tag");
//                                    endianness = Endianness.LITTLE;
//                                    log("Endianness autodetected as little-endian");
//                                } catch (Exception e2) {
//                                    e2.addSuppressed(e);
//                                    throw e2;
//                                }
//                            }
//                        }
//                    }
//                }
//                if (tag == null) throw new RuntimeException("Got null root tag");
//            } catch (Exception e) {
//                log("Exception while trying to load NBT file", e);
//                System.err.println("unbted: Failed to load " + (sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
//                if (!compressionAutodetected) {
//                    System.err.println("unbted: Are you sure " + compressionMethod + " is the correct compression method?");
//                    if (detectedCompressionMethod != compressionMethod) {
//                        System.err.println("unbted: It looks like " + detectedCompressionMethod + " to me");
//                    }
//                } else if (isJson) {
//                    System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
//                } else {
//                    System.err.print("unbted: Are you sure this is an NBT file?");
//                    if (endianness != null) {
//                        if (endianness == Endianness.ZZAZZ) {
//                            System.err.print(" (Maybe it's not in a joke format?)");
//                        } else {
//                            System.err.print(" (Maybe it's ");
//                            System.err.print(endianness == Endianness.LITTLE ? "big" : "little");
//                            System.err.print("-endian?)");
//                        }
//                    }
//                    System.err.println();
//                }
//                System.exit(2);
//                return;
//            }
//        } else {
//            endianness = Endianness.BIG;
//        }
//        // allow gc, especially for fully-buffered stdin
//        inSupplier = null;
//        TagPrinter printer = new TagPrinter(System.out);
//        if (!set.has("no-print")) {
//            if (JSON_MODE != JsonMode.NONE) {
//                JsonElement e = toJson(tag, JSON_MODE == JsonMode.ROUNDTRIP);
//                if (JSON_MODE == JsonMode.ROUNDTRIP) {
//                    JsonObject obj = new JsonObject();
//                    obj.addProperty("_unbted", 1);
//                    obj.addProperty("rootType", getTypePrefix(tag));
//                    obj.addProperty("rootName", tag.getName());
//                    obj.add("root", e);
//                    e = obj;
//                }
//                StringWriter sw = new StringWriter();
//                JsonWriter jw = new JsonWriter(sw);
//                jw.setIndent("  ");
//                jw.setLenient(true);
//                gson.toJson(e, jw);
//                aout.println(sw);
//            } else {
//                printer.printTag(tag, "", INFER, RecurseMode.FULL);
//            }
//        }
//        if (!set.has("print")) {
//            initializeTerminal();
//            System.err.println("Una's NBT Editor v" + VERSION);
//            System.err.println("Copyright (C) 2018 - 2023 Una Thompson (unascribed)");
//            System.err.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `warranty`.");
//            System.err.println("This is free software, and you are welcome to redistribute it under certain");
//            System.err.println("conditions; type `copying` for details.");
//            System.err.println();
//            System.err.println("Type `help` for help");
//            CommandProcessor cp = new CommandProcessor(tag, printer, new FileInfo(sourceFile, compressionMethod, compressionAutodetected, endianness, isJson));
//            cp.run();
//        }
    }

    private static void initializeTerminal() throws IOException {
        AnsiConsole.systemInstall();
        terminal = TerminalBuilder.builder()
                .jansi(true)
                .color(true)
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                AnsiConsole.systemUninstall();
                terminal.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private NBTTag loadJson(InputStream is) {
        JsonObject json = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
        JsonElement unbtedMarker = json.get("_unbted");
        if (unbtedMarker != null) {
            int version = unbtedMarker.getAsInt();
            if (version > 1) {
                System.err.println("unbted: This looks like an NBT JSON file, but it's of a version newer than I know how to read. (" + version + ")");
                System.err.println("unbted: Aborting.");
                System.exit(2);
                return null;
            } else {
                log("Looks like NBT JSON");
                return fromJson(json.get("rootType").getAsString() + ":" + json.get("rootName").getAsString(), json.get("root"));
            }
        } else {
            System.err.println("unbted: This looks like a JSON file, but it's not an NBT JSON file.");
            System.err.println("unbted: Aborting.");
            System.exit(2);
            return null;
        }
    }

    public static String getTypePrefix(NBTTag tag) {
        if (tag == null) {
            return "null";
        } else if (tag instanceof NBTList li) {
            if (li.getElementType() != null) {
                return "list<" + getTypePrefix(li.get(0)) + ">";
            }
            return "list<?>";
        } else {
            return NBTRegistry.typeNameForTag(tag);
        }
    }

    private static NBTTag fromJson(String name, JsonElement ele) {
        String[] split = name.split(":");
        if(split.length != 2) throw new IllegalArgumentException("All keys in an unbted NBT JSON file must be prefixed with their type");

        String type = split[0];
        String key = split[1];

        return switch(type) {
            case "null" -> null;
            case "byte" -> new NBTByte(key, ele.getAsByte());
            case "double" -> new NBTDouble(key, ele.getAsDouble());
            case "float" -> new NBTFloat(key, ele.getAsFloat());
            case "int" -> new NBTInt(key, ele.getAsInt());
            case "long" -> new NBTLong(key, ele.getAsLong());
            case "short" -> new NBTShort(key, ele.getAsShort());
            case "string" -> new NBTString(key, ele.getAsString());
            case "byte-array" -> new NBTByteArray(key, Base64.getDecoder().decode(ele.getAsString()));
            case "int-array" -> {
                JsonArray arr = ele.getAsJsonArray();
                int[] out = new int[arr.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = arr.get(i).getAsInt();
                }
                yield new NBTIntArray(key, out);
            }
            case "long-array" -> {
                JsonArray arr = ele.getAsJsonArray();
                long[] out = new long[arr.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = arr.get(i).getAsLong();
                }
                yield new NBTLongArray(key, out);
            }
            case "compound" -> {
                NBTCompound out = new NBTCompound(key);
                for (Map.Entry<String, JsonElement> en : ele.getAsJsonObject().entrySet()) {
                    if ("_unbted".equals(en.getKey())) continue;
                    out.put(fromJson(en.getKey(), en.getValue()));
                }
                yield out;
            }
            default -> {
                if (type.startsWith("list<")) {
                    int closer = type.lastIndexOf('>');
                    if (closer == -1) {
                        throw new IllegalArgumentException("Expected closing > in list type, didn't find one (for " + type + ")");
                    }
                    String innerType = type.substring(5, closer);
                    if ("?".equals(innerType)) {
                        if (ele == null || ele.getAsJsonArray().isEmpty()) {
                            yield new NBTList(key);
                        } else {
                            throw new IllegalArgumentException("Cannot have list of unknown type with elements");
                        }
                    } else {
                        NBTList out = new NBTList(key);
                        for (JsonElement child : ele.getAsJsonArray()) {
                            out.add(fromJson(innerType + ":", child));
                        }
                        yield out;
                    }
                }

                throw new IllegalArgumentException("Unknown type " + type + " when parsing key " + type + ":" + key);
            }
        };
    }

    public static JsonElement toJson(NBTTag tag, boolean roundTrip) {
        if(tag == null) return JsonNull.INSTANCE;

        return switch(tag) {
            case NBTCompound compound -> JSONFromNBT.fromNBTCompound(roundTrip, compound);
            case NBTList list -> JSONFromNBT.fromNBTList(roundTrip, list);
            case NBTNumber number -> new JsonPrimitive(number.numberValue());
            case NBTString string -> new JsonPrimitive(string.stringValue());
            case NBTByteArray bArr -> new JsonPrimitive(Base64.getEncoder().encodeToString(bArr.getValue()));
            case NBTIntArray iArr -> JSONFromNBT.fromIntArr(roundTrip, iArr);
            case NBTLongArray lArr -> JSONFromNBT.fromLongArr(lArr);
            default -> {
                throw new IllegalArgumentException("Don't know how to convert " + tag.getClass().getSimpleName() + " to JSON");
            }
        };
    }

    private static final class JSONFromNBT {
        private JSONFromNBT() {}

        private static JsonElement fromNBTCompound(boolean roundTrip, NBTCompound compound) {
            JsonObject out = new JsonObject();
            for (NBTTag t : compound.values()) {
                out.add((roundTrip ? getTypePrefix(t) + ":" : "") + t.getName(), toJson(t, roundTrip));
            }
            if (!roundTrip) {
                List<String> keys = new ArrayList<>(out.keySet());
                Collections.sort(keys);
                JsonObject sorted = new JsonObject();
                for (String k : keys) {
                    if (k.endsWith("Least") && sorted.has(k.replaceFirst("Least$", ""))) {
                        continue;
                    }
                    if (k.endsWith("Most") && out.has(k.replaceFirst("Most$", "Least"))) {
                        String basek = k.replaceFirst("Most$", "");
                        String k2 = basek + "Least";
                        if (out.get(k) instanceof JsonPrimitive p1 && out.get(k2) instanceof JsonPrimitive p2) {
                            if (p1.isNumber() && p2.isNumber()) {
                                sorted.add(basek, new JsonPrimitive(new UUID(p1.getAsLong(), p2.getAsLong()).toString()));
                                continue;
                            }
                        }
                    }
                    sorted.add(k, out.get(k));
                }
                out = sorted;
            }
            return out;
        }

        private static JsonElement fromNBTList(boolean roundTrip, NBTList list) {
            JsonArray out = new JsonArray();
            for (NBTTag t : list) {
                out.add(toJson(t, roundTrip));
            }
            return out;
        }

        private static JsonElement fromIntArr(boolean roundTrip, NBTIntArray arr) {
            if (!roundTrip && arr.size() == 4) {
                return new JsonPrimitive(UUIDs.fromIntArray(arr.getValue()).toString());
            }
            JsonArray out = new JsonArray();
            for (int v : arr.getValue()) {
                out.add(v);
            }
            return out;
        }

        private static JsonElement fromLongArr(NBTLongArray arr) {
            JsonArray out = new JsonArray();
            for (long v : arr.getValue()) {
                out.add(v);
            }
            return out;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: unbted [options] [file]");
        System.err.println("See `unbted --help` for detailed usage information");
    }

    private static void printHelp() {
        displayEmbeddedFileInPager("switches-help.txt");
    }

    public static void displayEmbeddedFileInPager(String file) {
        try (var br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(file)))) {
            System.err.println(br.lines().collect(Collectors.joining("\n")));
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {

    }
}
