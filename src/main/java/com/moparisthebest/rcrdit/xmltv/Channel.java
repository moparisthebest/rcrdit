/*
 * rcrdit records TV programs from TV tuners
 * Copyright (C) 2017  Travis Burtrum
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moparisthebest.rcrdit.xmltv;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Created by mopar on 2/17/17.
 */
public class Channel {
    //@JacksonXmlProperty(localName = "id", isAttribute = true)
    private final String id;

    //@JsonProperty("display-name")
    //@JacksonXmlElementWrapper(localName = "display-name", useWrapping = false)
    private final Set<String> displayNames;

    public Channel(final String id, final Set<String> displayNames) {
        this.id = id;
        this.displayNames = Collections.unmodifiableSet(displayNames);
    }

    public String getId() {
        return id;
    }

    public Set<String> getDisplayNames() {
        return displayNames;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Channel)) return false;
        final Channel channel = (Channel) o;
        return Objects.equals(id, channel.id) &&
                Objects.equals(displayNames, channel.displayNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayNames);
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id='" + id + '\'' +
                ", displayNames=" + displayNames +
                '}';
    }
}
