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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by mopar on 2/26/17.
 */
public class ExecThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ExecThread.class);

    private final boolean recordingStart;
    private final File outputFile;
    private final ProgramAutoRec recording;

    ExecThread(final boolean recordingStart, final File outputFile, final ProgramAutoRec recording) {
        this.recordingStart = recordingStart;
        this.outputFile = outputFile;
        this.recording = recording;
    }

    @Override
    public void run() {
        final String cmd = recordingStart ? recording.getAutoRec().getProfile().getRunAtRecordingStart() : recording.getAutoRec().getProfile().getRunAtRecordingFinish();
        if (cmd == null)
            return;
        final Runtime runtime = Runtime.getRuntime();
        try {
            final Process process = runtime.exec(new String[]{cmd, outputFile.getAbsolutePath()});
            new DevNullThread(process.getInputStream()).start();
            new DevNullThread(process.getErrorStream()).run(); // we just .run() here to keep it in same thread instead of .start()
            process.waitFor(); // don't care about exit status, what could we do?
        } catch (IOException | InterruptedException e) {
            log.error("error executing process " + this, e);
        }
    }
}
