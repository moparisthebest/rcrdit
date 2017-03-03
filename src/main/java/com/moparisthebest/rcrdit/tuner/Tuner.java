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

package com.moparisthebest.rcrdit.tuner;

import com.moparisthebest.rcrdit.autorec.ProgramAutoRec;

import java.util.Timer;

/**
 * Created by mopar on 2/17/17.
 */
public interface Tuner extends AutoCloseable {
    boolean recordNow(ProgramAutoRec par, Timer timer);

    boolean stopRecording(ProgramAutoRec par);

    boolean isRecording(ProgramAutoRec par);

    boolean stopAndRecordNow(ProgramAutoRec stop, ProgramAutoRec start, Timer timer);

    @Override
    public void close();

    default ProgramAutoRec getRecording() {
        throw new UnsupportedOperationException();
    }

    default int getPriority() {
        throw new UnsupportedOperationException();
    }
}
