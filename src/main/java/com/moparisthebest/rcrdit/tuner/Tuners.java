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
import com.moparisthebest.rcrdit.xmltv.Program;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Timer;

/**
 * Created by mopar on 2/18/17.
 */
public class Tuners implements Tuner {
    private final List<Tuner> tuners;
    private final int numTuners;

    public Tuners(final List<Tuner> tuners) {
        this.tuners = tuners;
        this.numTuners = tuners.size();
        if (this.numTuners == 0)
            throw new IllegalArgumentException("Must have at least one tuner");
    }

    public int numTuners() {
        return numTuners;
    }

    @Override
    public synchronized boolean recordNow(final ProgramAutoRec par, final Timer timer) {
        Objects.requireNonNull(par, "Cannot record null program");
        final Program program = par.getProgram();
        // first look for tuners already recording this channel, use it if found
        Tuner chosenTuner = tuners.stream().filter(r -> r.getRecording() != null && program.getChannelName().equals(r.getRecording().getProgram().getChannelName())).findFirst().orElseGet(
                // look for un-used tuners
                () -> tuners.stream().filter(r -> r.getRecording() == null).findFirst().orElseGet(
                        // look for current recordings ending within 70 seconds
                        () -> {
                            final Instant oneMinuteInFuture = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(70);
                            return tuners.stream().filter(r -> r.getRecording() == null || r.getRecording().getProgram().getStop().isBefore(oneMinuteInFuture)).findFirst().orElseGet(
                                    // find lowest priority
                                    () -> tuners.stream().min(Comparator.comparingInt(Tuner::getPriority)).get()
                            );
                        }
                )
        );
        // use the above monstrosity of lamda instead of the following procedural programming...
        if (true)
            return chosenTuner.recordNow(par, timer);
        // first look for tuners already recording this channel, use it if found
        for (final Tuner tuner : tuners) {
            final Program recording = tuner.getRecording().getProgram();
            if (recording != null && program.getChannelName().equals(recording.getChannelName())) { // || tuner.isRecording(program) is redundant
                chosenTuner = tuner;
                break;
            }
        }
        // look for un-used tuners
        if (chosenTuner == null)
            for (final Tuner tuner : tuners) {
                if (tuner.getRecording() == null) {
                    chosenTuner = tuner;
                    break;
                }
            }
        // look for current recordings ending within 70 seconds
        if (chosenTuner == null) {
            final Instant oneMinuteInFuture = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(70);
            for (final Tuner tuner : tuners) {
                final Program recording = tuner.getRecording().getProgram();
                if (recording == null || recording.getStop().isBefore(oneMinuteInFuture)) {
                    chosenTuner = tuner;
                    break;
                }
            }
        }
        // find lowest priority
        if (chosenTuner == null) {
            chosenTuner = tuners.get(0);
            for (final Tuner tuner : tuners) {
                if (chosenTuner.getPriority() > tuner.getPriority()) {
                    chosenTuner = tuner;
                    break;
                }
            }
        }
        return chosenTuner.recordNow(par, timer);
    }

    public List<Tuner> getTuners() {
        return tuners;
    }
    
    

    @Override
    public synchronized boolean stopRecording(final ProgramAutoRec program) {
        for (final Tuner tuner : tuners)
            if (tuner.stopRecording(program))
                return true;
        return false;
    }

    @Override
    public synchronized boolean stopAndRecordNow(final ProgramAutoRec stop, final ProgramAutoRec program, final Timer timer) {
        for (final Tuner tuner : tuners)
            if (tuner.isRecording(program))
                return tuner.stopAndRecordNow(stop, program, timer);
        // this.stopRecording(stop); // no need to call this because it isn't being recorded per loop above
        return recordNow(program, timer);
    }

    @Override
    public synchronized boolean isRecording(final ProgramAutoRec program) {
        for (final Tuner tuner : tuners)
            if (tuner.isRecording(program))
                return true;
        return false;
    }

    @Override
    public void close() {
        for (final Tuner tuner : tuners)
            tuner.close();
    }

    @Override
    public String toString() {
        return "Tuners{" +
                "tuners=" + tuners +
                ", numTuners=" + numTuners +
                '}';
    }
}
