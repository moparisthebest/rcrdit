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

import java.io.InputStream;
import java.net.URL;
import java.util.Timer;

/**
 * Created by mopar on 2/17/17.
 */
public class HDHomerun extends AbstractTuner {

    private static final Logger log = LoggerFactory.getLogger(HDHomerun.class);

    protected static final String ext = "ts";

    protected final String url;

    protected Thread thread = null;

    public HDHomerun(final String url) {
        this.url = url;
    }

    public HDHomerun(final HDHomerun other) {
        this(other.url);
    }

    @Override
    public synchronized boolean recordNow(final ProgramAutoRec program, final Timer timer) {
        final boolean changedRecording = super.recordNow(program, timer);
        if (changedRecording) {
            if (thread != null)
                thread.interrupt();
            thread = new RecordThread(program, ext, 16 * 1024, this::getInputStream);
            thread.start();
        }
        return changedRecording;
    }

    @Override
    public synchronized boolean stopRecording(final ProgramAutoRec program) {
        final boolean stoppedRecording = super.stopRecording(program);
        if (stoppedRecording && thread != null) {
            thread.interrupt();
            thread = null;
        }
        return stoppedRecording;
    }

    private InputStream getInputStream(final ProgramAutoRec pac) {
        try {
            final URL url = new URL(String.format(this.url, pac.getProgram().getChannelName()));
            return url.openStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "HDHomerun{" +
                "url='" + url + '\'' +
                "} " + super.toString();
    }
}
