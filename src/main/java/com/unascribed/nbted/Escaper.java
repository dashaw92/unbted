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

import java.util.HashMap;
import java.util.Map;

public final class Escaper {

    private final Map<Character, CharSequence> escapes = new HashMap<>();

    public Escaper addEscape(char target, CharSequence replace) {
        escapes.put(target, replace);
        return this;
    }

    public String escape(String input) {
        StringBuilder current = new StringBuilder();
        for(char c : input.toCharArray()) {
            current.append(escapes.getOrDefault(c, "" + c));
        }
        return current.toString();
    }
}
