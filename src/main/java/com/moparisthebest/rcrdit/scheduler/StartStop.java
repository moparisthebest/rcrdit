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

package com.moparisthebest.rcrdit.scheduler;

import com.moparisthebest.rcrdit.xmltv.Program;

import java.time.Instant;
import java.util.Objects;

/**
 * Created by mopar on 3/14/17.
 */
public class StartStop {
    private final Instant instant;
    private final boolean start;
    private final Program toStop;

    public StartStop(final Instant instant, final boolean start, final Program toStop) {
        this.instant = instant;
        this.start = start;
        this.toStop = toStop;
    }

    public StartStop(final Instant instant, final boolean start) {
        this(instant, start, null);
    }

    public Instant getInstant() {
        return instant;
    }

    public boolean isStart() {
        return start;
    }

    public Program getToStop() {
        return toStop;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof StartStop)) return false;
        final StartStop startStop = (StartStop) o;
        return start == startStop.start &&
                Objects.equals(instant, startStop.instant) &&
                Objects.equals(toStop, startStop.toStop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instant, start, toStop);
    }

    @Override
    public String toString() {
        return "StartStop{" +
                "instant=" + instant +
                ", start=" + start +
                ", toStop=" + toStop +
                '}';
    }
}
