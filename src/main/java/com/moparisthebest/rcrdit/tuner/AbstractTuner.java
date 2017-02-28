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

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mopar on 2/17/17.
 */
public abstract class AbstractTuner implements Tuner {

    private static final Logger log = LoggerFactory.getLogger(AbstractTuner.class);

    protected ProgramAutoRec recording = null;
    protected TimerTask recordingEnd = null;

    @Override
    public synchronized boolean recordNow(final ProgramAutoRec program, final Timer timer) {
        Objects.requireNonNull(program, "Cannot record null program");
        if (this.recording != null) {
            // if program is the same just change autorec so priority changes
            if (this.recording.getProgram().equals(program.getProgram())) {
                this.recording = program;
                return false;
            }
            this.stopRecording(this.recording);
        }
        log.debug("starting to record program '{}' at '{}'", program, Instant.now().toString());
        this.recording = program;
        this.recordingEnd = new RecordingEnd(program, this);
        timer.schedule(this.recordingEnd, Date.from(recording.getProgram().getStop()));
        return true;
    }

    @Override
    public synchronized boolean stopRecording(final ProgramAutoRec program) {
        if (this.isRecording(program)) {
            log.debug("stopping recording program '{}' at '{}'", program, Instant.now().toString());
            this.recordingEnd.cancel();
            this.recording = null;
            this.recordingEnd = null;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean stopAndRecordNow(final ProgramAutoRec stop, final ProgramAutoRec program, final Timer timer) {
        log.debug("replacing recording program '{}' at '{}' stop '{}' with program '{}'", program, Instant.now().toString(), stop, program);
        this.stopRecording(stop);
        return recordNow(program, timer);
    }

    @Override
    public boolean isRecording(final ProgramAutoRec program) {
        final ProgramAutoRec recording = this.recording; // atomic so no races
        return recording != null && recording.getProgram().equals(program.getProgram());
    }

    @Override
    public ProgramAutoRec getRecording() {
        return this.recording;
    }

    @Override
    public int getPriority() {
        final ProgramAutoRec recording = this.recording; // atomic so no races
        return recording == null ? -1 : recording.getAutoRec().getPriority();
    }

    @Override
    public void close() {
        this.stopRecording(this.recording);
    }

    @Override
    public String toString() {
        return "AbstractTuner{" +
                "recording=" + recording +
                ", recordingEnd=" + recordingEnd +
                '}';
    }
}
