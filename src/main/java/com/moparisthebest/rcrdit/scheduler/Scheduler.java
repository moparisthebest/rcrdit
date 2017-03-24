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

import com.moparisthebest.rcrdit.autorec.AutoRec;
import com.moparisthebest.rcrdit.xmltv.Program;

import java.time.Instant;
import java.util.List;

/**
 * Created by mopar on 3/14/17.
 */
public interface Scheduler {
    void schedulePrograms(final List<AutoRec> autoRecs, final List<Program> programs, final int numTuners, final Instant end, final Instant startAt);
}
