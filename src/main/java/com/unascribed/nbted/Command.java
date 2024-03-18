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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Command {
    private AliasAwareAction action;
    private Completer completer;
    private Options options;
    private String description = "";
    private String name = "";
    private String usage = "";
    private List<String> aliases = List.of();
    private List<String> allNames = List.of();
    private Command() {
    }

    public static Command create() {
        return new Command();
    }

    public void execute(String alias, Iterable<String> args) throws Exception {
        if (action == null) return;
        execute(alias, StreamSupport.stream(args.spliterator(), false).toArray(String[]::new));
    }

    public void execute(String alias, String... args) throws Exception {
        if (action == null) return;
        OptionParser parser = new OptionParser();
        parser.posixlyCorrect(System.getenv("POSIXLY_CORRECT") != null);
        setupOptionParser(parser);
        OptionSet set;
        try {
            set = parser.parse(args);
        } catch (OptionException e) {
            throw new CommandException(CommandException.VALUE_BAD_USAGE, e.getMessage());
        }
        execute(alias, set);
    }

    public void execute(String alias, OptionSet set) throws Exception {
        if (action == null) return;
        action.run(alias, set, (List<String>) set.nonOptionArguments());
    }

    public void setupOptionParser(OptionParser parser) throws Exception {
        if (options != null) {
            options.setup(parser);
        }
    }

    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (completer != null) {
            completer.complete(reader, line, candidates);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getUsage(String alias) {
        return usage.replace("{}", alias);
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getAllNames() {
        return allNames;
    }

    public Command action(AliasUnawareAction action) {
        this.action = (alias, set, args) -> action.run(set, args);
        return this;
    }

    public Command action(AliasAwareAction action) {
        this.action = action;
        return this;
    }

    public Command completer(Completer completer) {
        this.completer = completer;
        return this;
    }

    public Command options(Options options) {
        this.options = options;
        return this;
    }

    public Command description(String description) {
        this.description = description;
        return this;
    }

    public Command name(String name) {
        this.name = name;
        this.allNames = Stream.concat(Stream.of(name), this.aliases.stream()).toList();
        return this;
    }

    public Command aliases(String... aliases) {
        this.aliases = List.of(aliases);
        this.allNames = Stream.concat(Stream.of(name), this.aliases.stream()).toList();
        return this;
    }

    public Command usage(String usage) {
        this.usage = usage;
        return this;
    }

    public interface AliasUnawareAction {
        void run(OptionSet set, List<String> args) throws Exception;
    }

    public interface AliasAwareAction {
        void run(String alias, OptionSet set, List<String> args) throws Exception;
    }


    public interface Options {
        void setup(OptionParser parser) throws Exception;
    }

}
