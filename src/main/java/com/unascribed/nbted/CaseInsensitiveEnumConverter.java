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

import joptsimple.ValueConverter;

import java.util.Locale;

public class CaseInsensitiveEnumConverter<E extends Enum<E>> implements ValueConverter<E> {

    private final Class<E> clazz;

    public CaseInsensitiveEnumConverter(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public E convert(String value) {
        return E.valueOf(clazz, value.toUpperCase(Locale.ROOT));
    }

    @Override
    public Class<? extends E> valueType() {
        return clazz;
    }

    @Override
    public String valuePattern() {
        return clazz.getSimpleName().toLowerCase(Locale.ROOT);
    }

}
