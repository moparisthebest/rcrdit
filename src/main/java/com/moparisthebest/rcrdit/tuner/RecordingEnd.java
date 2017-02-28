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

import java.util.TimerTask;

/**
 * Created by mopar on 2/18/17.
 */
public class RecordingEnd extends TimerTask {
    private final ProgramAutoRec recording;
    private final Tuner tuner;

    RecordingEnd(final ProgramAutoRec recording, final Tuner tuner) {
        this.recording = recording;
        this.tuner = tuner;
    }

    @Override
    public void run() {
        this.tuner.stopRecording(recording);
        this.cancel();
    }
}
