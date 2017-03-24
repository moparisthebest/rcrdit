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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Created by mopar on 3/14/17.
 */
public class PartialScheduler implements Scheduler {
    
    private static final Logger log = LoggerFactory.getLogger(PartialScheduler.class);
    
    @Override
    public void schedulePrograms(final List<AutoRec> autoRecs, final List<Program> programs, final int numTuners, final Instant end, final Instant startAt) {
        if (programs.isEmpty() || autoRecs.isEmpty()) {
            log.debug("nothing to import.");
            return;
        }
        // start now, get all matching programs scheduled for that minute, determine which to record, schedule timers, start any now?  make sure current ones don't get reset
        Instant cursor = startAt;
        //final Instant end = cursor.plus(1, ChronoUnit.DAYS);
        final Map<Program, Instant> currentRecs = new LinkedHashMap<>(numTuners); // todo: needs to be sorted?
        final List<Program> programAutoRecs = new ArrayList<>();
        log.debug("end: {}", end);
        Program lastProgram = null, lastNonTimeProgram = null;
        while (!cursor.isAfter(end)) {
            programAutoRecs.clear();
            for (final Program program : programs) {
                lastNonTimeProgram = program;
                if (!cursor.isBefore(program.getStart()) && cursor.isBefore(program.getStop())) {
                    lastProgram = program;
                    // this program is on
                    for (final AutoRec autoRec : autoRecs) {
                        if (autoRec.matches(program)) {
                            programAutoRecs.add(program.setAutoRec(autoRec));
                            break; // only match highest priority autorec, ie first since they are sorted
                        }
                    }
                }
            }
            // schedule top 2 (numTuners), record stuff about rest, schedule only if not already recording ouch?
            if (!programAutoRecs.isEmpty()) {
                programAutoRecs.sort(Program::compareTo);
                // start and stop are same minute and second
                final Instant finalCursor = cursor;

                // it already ended, and stopped itself, just remove it, for both currentRecs and skippedRecs
                //recs.keySet().removeIf(c -> c.getProgram().getStop().isBefore(finalCursor));
                for (final Iterator<Map.Entry<Program, Instant>> it = currentRecs.entrySet().iterator(); it.hasNext(); ) {
                    final Map.Entry<Program, Instant> entry = it.next();
                    final Program prog = entry.getKey();
                    if (!prog.getStop().isAfter(finalCursor)) {
                        //if(prog.getStop().isBefore(finalCursor)) {
                        final Instant start = entry.getValue();
                        prog.addStartStop(new StartStop(prog.getStop(), false));
                        it.remove();
                    }
                }

                // look at highest programAutoRecs up to the number of tuners
                for (int x = 0; x < Math.min(programAutoRecs.size(), numTuners); ++x) {
                    final Program programAutoRec = programAutoRecs.get(x);
                    // if we are already recording it, move on
                    if (currentRecs.containsKey(programAutoRec))
                        continue;
                    // free tuner, just add
                    if (currentRecs.size() < numTuners) {
                        currentRecs.put(programAutoRec, cursor);
                        programAutoRec.addStartStop(new StartStop(cursor, true));
                        continue;
                    }
                    // from Tuners.recordNow
                    final Program recToReplace = currentRecs.keySet().stream().filter(r -> r != null && programAutoRec.getChannelName().equals(r.getChannelName())).findFirst().orElseGet(
                            // look for un-used tuners
                            () -> currentRecs.keySet().stream().filter(Objects::isNull).findFirst().orElseGet(
                                    // look for current recordings ending within 70 seconds
                                    () -> {
                                        final Instant oneMinuteInFuture = finalCursor.plusSeconds(70);
                                        return currentRecs.keySet().stream().filter(r -> r == null || r.getStop().isBefore(oneMinuteInFuture)).findFirst().orElseGet(
                                                // find lowest priority
                                                () -> currentRecs.keySet().stream().min(Comparator.comparingInt(cr -> cr.getAutoRec().getPriority())).orElse(null)
                                        );
                                    }
                            )
                    );
                    //System.out.println("replacing: "+ recToReplace + " with scheduling: "+programAutoRec);
                    if(programAutoRec.equals(recToReplace)) { // todo: remove this cause should never happen due to currentRecs.containsKey(programAutoRec) above, just testing though...
                        System.out.println("wtf...");
                        continue;
                    }
                    recToReplace.addStartStop(new StartStop(cursor, false));
                    programAutoRec.addStartStop(new StartStop(cursor, true, recToReplace));
                    // remove/replace it
                    final Instant start = currentRecs.remove(recToReplace);
                    currentRecs.put(programAutoRec, cursor);
                }

                //System.out.println(cursor);
                //System.out.println(programAutoRecs);
            }
            cursor = cursor.plus(1, ChronoUnit.MINUTES);
        }
        log.debug("cursor: {}", cursor);
        log.debug("lastProgram: {}", lastProgram);
        log.debug("lastNonTimeProgram: {}", lastNonTimeProgram);
        // increment by minute up until end scheduling all timers until then, somehow store this info to show/email etc?

        log.debug("import done.\n\n------\n\n");
    }
}
