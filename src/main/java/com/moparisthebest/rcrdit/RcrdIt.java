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

package com.moparisthebest.rcrdit;

import com.moparisthebest.jdbc.QueryMapper;
import com.moparisthebest.rcrdit.autorec.AutoRec;
import com.moparisthebest.rcrdit.autorec.Profile;
import com.moparisthebest.rcrdit.autorec.ProgramAutoRec;
import com.moparisthebest.rcrdit.tuner.HDHomerun;
import com.moparisthebest.rcrdit.tuner.Tuner;
import com.moparisthebest.rcrdit.tuner.Tuners;
import com.moparisthebest.rcrdit.xmltv.Program;
import com.moparisthebest.rcrdit.xmltv.Tv;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.*;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import com.moparisthebest.sxf4j.impl.AbstractXmlElement;
import com.moparisthebest.sxf4j.impl.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mopar on 2/16/17.
 */
@ApplicationPath("rest")
@Path("")
public class RcrdIt extends ResourceConfig implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RcrdIt.class);

    // config items
    private final Tuners tuners;
    private final Set<String> allChannels;
    private final List<String> xmltvPaths;
    private final String databaseUrl;

    private final TimerTask scheduleTask = new ScheduleTask();
    private final Timer timer = new Timer();

    private final List<TimerTask> startTimers = new ArrayList<>();
    private final List<AutoRec> autoRecs = new ArrayList<>();
    private Tv schedule;

    private final String serverUri;


    public RcrdIt(final File cfg) throws Exception {
        register(this);
        final XmlElement rcrdit = AbstractXmlElement.getFactory().readFromFile(cfg);
        this.databaseUrl = rcrdit.getChild("databaseUrl").getValue();

        // this needs to end in /
        String serverUri = rcrdit.getChild("serverUri").getValue();
        if (!serverUri.endsWith("/"))
            serverUri += "/";
        this.serverUri = serverUri;

        this.xmltvPaths = Arrays.stream(rcrdit.getChild("xmltvPaths").getChildren("xmltvPath")).map(XmlElement::getValue).collect(Collectors.toList());
        this.allChannels = Arrays.stream(rcrdit.getChild("channels").getChildren("channel")).map(XmlElement::getValue).collect(Collectors.toSet());

        final List<Tuner> tuners = new ArrayList<>();
        for (final XmlElement tuner : rcrdit.getChild("tuners").getChildren("tuner")) {
            switch (tuner.getAttribute("type")) {
                case "HDHomeRun":
                    tuners.add(new HDHomerun(tuner.getAttribute("url")));
                    break;
                default:
                    throw new IllegalArgumentException("unknown tuner type");
            }
        }
        this.tuners = new Tuners(tuners);

        //tuners.forEach(t -> allChannels.addAll(t.getChannels()));
        //scheduleTask.run(); log.debug(this.toString()); System.exit(0);

        // snatch autorecs
        timer.schedule(new AutoRecTask(), 0);
        // import schedule
        timer.scheduleAtFixedRate(scheduleTask, 0, 12 * 60 * 60 * 1000); // every 12 hours?

        /*
        for(final Channel chan : schedule.getChannels())
            for(final String name : chan.getDisplayNames())
                if(name.length() < 5)
                    System.out.print("\"" + name + "\", ");
        */
    }

    private static void addMeeting(final Calendar calendar, final VTimeZone tz,
                                   final Instant start, final Instant stop,
                                   final ProgramAutoRec prog, final MessageDigest md, final boolean skipped) {

        String name = prog.getProgram().getTitle();
        String description = prog.getProgram().getDesc();

        if (skipped) {
            // since end and begin are the same we get alot of these for the same minute, ignore them
            if (start.equals(stop))
                return;
            name = "SKIPPED: " + name;
        }
        // Create the event
        final VEvent meeting = new VEvent(new DateTime(start.toEpochMilli()), new DateTime(stop.toEpochMilli()), name);
        meeting.getProperties().add(new Location(prog.getProgram().getChannelName()));

        final Duration oneMinute = Duration.of(1, ChronoUnit.MINUTES);
        if (Duration.between(start, prog.getProgram().getStart()).abs().compareTo(oneMinute) > 0)
            description += "\nMISSING BEGINNING";
        if (Duration.between(stop, prog.getProgram().getStop()).abs().compareTo(oneMinute) > 0)
            description += "\nMISSING ENDING";

        description += "\nPriority: " + prog.getAutoRec().getPriority();

        meeting.getProperties().add(new Description(description));

        // add timezone info..
        meeting.getProperties().add(tz.getTimeZoneId());

        // generate unique identifier..
        md.reset();
        // incorporate everything to make this unique
        md.update(Long.toString(start.toEpochMilli()).getBytes());
        md.update(Long.toString(stop.toEpochMilli()).getBytes());
        md.update(Long.toString(prog.getProgram().getStart().toEpochMilli()).getBytes());
        md.update(Long.toString(prog.getProgram().getStop().toEpochMilli()).getBytes());
        md.update(prog.getProgram().getChannelName().getBytes());
        md.update(name.getBytes());
        md.update(description.getBytes());
        final byte[] digest = md.digest();
        final Uid uid = new Uid(String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest)));
        /*
        UidGenerator ug = null;
        try {
            ug = new UidGenerator("uidGen");
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Uid uid = ug.generateUid();
        */
        meeting.getProperties().add(uid);

        if (skipped) {
            // mark cancelled
            meeting.getProperties().add(Status.VEVENT_CANCELLED);
        }

        calendar.getComponents().add(meeting);
    }

    private synchronized void reCalculateSchedule() {
        log.debug("import starting.");
        // cancel all pending timers, clear array, purge timer
        startTimers.forEach(TimerTask::cancel);
        startTimers.clear();
        timer.purge();

        if (schedule.getPrograms().isEmpty() || autoRecs.isEmpty()) {
            log.debug("nothing to import.");
            return;
        }

        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no sha-256?", e);
        }
        // Create a TimeZone
        final VTimeZone tz = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(ZoneId.systemDefault().getId()).getVTimeZone();
        final Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//rcrdit//rcrdit//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        //addMeeting(calendar, tz, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "Progress Meeting", "this is a description", "22.1");


        // start now, get all matching programs scheduled for that minute, determine which to record, schedule timers, start any now?  make sure current ones don't get reset
        Instant cursor = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        //final Instant end = cursor.plus(1, ChronoUnit.DAYS);
        final Instant end = schedule.getLastEndTime();
        final Map<ProgramAutoRec, Instant> currentRecs = new LinkedHashMap<>(tuners.numTuners()); // todo: needs to be sorted?
        final Map<ProgramAutoRec, Instant> skippedRecs = new LinkedHashMap<>(tuners.numTuners()); // todo: needs to be sorted?
        final List<ProgramAutoRec> programAutoRecs = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            programAutoRecs.clear();
            for (final Program program : schedule.getPrograms()) {
                if (!cursor.isBefore(program.getStart()) && cursor.isBefore(program.getStop())) {
                    // this program is on
                    for (final AutoRec autoRec : autoRecs) {
                        if (autoRec.matches(program)) {
                            programAutoRecs.add(new ProgramAutoRec(program, autoRec));
                            break; // only match highest priority autorec, ie first since they are sorted
                        }
                    }
                }
            }
            // schedule top 2 (numTuners), record stuff about rest, schedule only if not already recording ouch?
            if (!programAutoRecs.isEmpty()) {
                programAutoRecs.sort(ProgramAutoRec::compareTo);
                // start and stop are same minute and second
                final Instant finalCursor = cursor;

                // it already ended, and stopped itself, just remove it, for both currentRecs and skippedRecs
                removeStoppedShows(tz, calendar, currentRecs, finalCursor, md, false);
                removeStoppedShows(tz, calendar, skippedRecs, finalCursor, md, true);

                // look at highest programAutoRecs up to the number of tuners
                for (int x = 0; x < Math.min(programAutoRecs.size(), tuners.numTuners()); ++x) {
                    final ProgramAutoRec programAutoRec = programAutoRecs.get(x);
                    // if we are already recording it, move on
                    if (currentRecs.containsKey(programAutoRec))
                        continue;
                    // free tuner, just add
                    if (currentRecs.size() < tuners.numTuners()) {
                        currentRecs.put(programAutoRec, cursor);
                        // check if we are starting one late, from skipped
                        final Instant start = skippedRecs.remove(programAutoRec);
                        if (start != null) {
                            addMeeting(calendar, tz, start, cursor, programAutoRec, md, true);
                        }
                        //System.out.println("scheduling: "+programAutoRec);
                        final RecordingTask rt = new RecordingTask(programAutoRec);
                        startTimers.add(rt);
                        timer.schedule(rt, Date.from(cursor));
                        continue;
                    }
                    // from Tuners.recordNow
                    final ProgramAutoRec recToReplace = currentRecs.keySet().stream().filter(r -> r.getProgram() != null && programAutoRec.getProgram().getChannelName().equals(r.getProgram().getChannelName())).findFirst().orElseGet(
                            // look for un-used tuners
                            () -> currentRecs.keySet().stream().filter(r -> r.getProgram() == null).findFirst().orElseGet(
                                    // look for current recordings ending within 70 seconds
                                    () -> {
                                        final Instant oneMinuteInFuture = finalCursor.plusSeconds(70);
                                        return currentRecs.keySet().stream().filter(r -> r.getProgram() == null || r.getProgram().getStop().isBefore(oneMinuteInFuture)).findFirst().orElseGet(
                                                // find lowest priority
                                                () -> currentRecs.keySet().stream().min(Comparator.comparingInt(cr -> cr.getAutoRec().getPriority())).orElse(null)
                                        );
                                    }
                            )
                    );
                    //System.out.println("replacing: "+ recToReplace + " with scheduling: "+programAutoRec);
                    final RecordingTask rt = new RecordingTask(recToReplace, programAutoRec);
                    startTimers.add(rt);
                    timer.schedule(rt, Date.from(cursor));

                    // remove/replace it
                    final Instant start = currentRecs.remove(recToReplace);
                    currentRecs.put(programAutoRec, cursor);
                    addMeeting(calendar, tz, start, cursor, recToReplace, md, false);
                    skippedRecs.put(recToReplace, cursor);
                }
                // look at the extra programAutoRecs that we are skipping due to no tuners
                for (int x = tuners.numTuners(); x < programAutoRecs.size(); ++x) {
                    skippedRecs.putIfAbsent(programAutoRecs.get(x), cursor);
                }

                //System.out.println(cursor);
                //System.out.println(programAutoRecs);
            }
            cursor = cursor.plus(1, ChronoUnit.MINUTES);
        }
        // increment by minute up until end scheduling all timers until then, somehow store this info to show/email etc?
        log.debug("import done.\n\n------\n\n");
        if (!calendar.getComponents().isEmpty())
            try (FileOutputStream fout = new FileOutputStream("rcrdit.ics")) {
                final CalendarOutputter outputter = new CalendarOutputter();
                outputter.output(calendar, fout);
            } catch (Exception e) {
                // ignore e.printStackTrace();
            }
    }

    private static void removeStoppedShows(final VTimeZone tz, final Calendar calendar, final Map<ProgramAutoRec, Instant> recs, final Instant finalCursor, final MessageDigest md, final boolean skipped) {
        //recs.keySet().removeIf(c -> c.getProgram().getStop().isBefore(finalCursor));
        for (final Iterator<Map.Entry<ProgramAutoRec, Instant>> it = recs.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<ProgramAutoRec, Instant> entry = it.next();
            final ProgramAutoRec par = entry.getKey();
            final Program prog = par.getProgram();
            if (!prog.getStop().isAfter(finalCursor)) {
                //if(prog.getStop().isBefore(finalCursor)) {
                final Instant start = entry.getValue();
                addMeeting(calendar, tz, start, prog.getStop(), par, md, skipped);
                it.remove();
            }
        }
    }

    @Override
    public void close() {
        timer.cancel();
        // todo: check if all threads terminated gracefully? how?
        tuners.close();
    }

    private class AutoRecTask extends TimerTask {
        @Override
        public void run() {
            log.debug("Connecting database...");
            try (Connection conn = DriverManager.getConnection(databaseUrl);
                 QueryMapper qm = new QueryMapper(conn)) {
                log.debug("Database connected!");
                final Map<Integer, Profile> profileMap = qm.toMap("SELECT profile_id, name, folder, run_at_recording_start, run_at_recording_finish FROM profile", new HashMap<>(), Integer.class, Profile.class);
                //System.out.println(profileMap);
                autoRecs.clear();
                qm.toCollection("SELECT profile_id, priority, title, channel_name, days_of_week, between_time_start, between_time_end, time_min, time_max FROM autorecs", autoRecs, AutoRec.class);
                autoRecs.forEach(a -> a.setProfile(profileMap.get(a.getProfileId())));
                //autoRecs.add(new AutoRec(null, 1, "Judge Judy", null, null, null, null, null, null));
                //autoRecs.add(new AutoRec(null, 5, "The Chew", null, null, null, null, null, null));
                autoRecs.sort(AutoRec::compareTo);
                //System.out.println(autoRecs);
                // recalculate every time but first since schedule hasn't been fetched yet
                if (schedule != null)
                    reCalculateSchedule();
            } catch (SQLException e) {
                e.printStackTrace();
                //throw new IllegalStateException("Cannot connect the database!", e);
            }
        }
    }

    private class ScheduleTask extends TimerTask {
        @Override
        public void run() {
            try {
                schedule = Tv.readSchedule(xmltvPaths, allChannels);
                //System.out.println(schedule);
                // todo: only re-calcuate if schedule changed
                reCalculateSchedule();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class RecordingTask extends TimerTask {
        private final ProgramAutoRec stop, start;

        RecordingTask(final ProgramAutoRec stop, final ProgramAutoRec start) {
            this.stop = stop;
            this.start = start;
        }

        RecordingTask(final ProgramAutoRec start) {
            this(null, start);
        }

        @Override
        public void run() {
            try {
                if (this.stop == null)
                    tuners.recordNow(this.start, timer);
                else
                    tuners.stopAndRecordNow(this.stop, this.start, timer);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "RcrdIt{" +
                "tuners=" + tuners +
                ", allChannels=" + allChannels +
                ", databaseUrl='" + databaseUrl + '\'' +
                ", xmltvPaths='" + xmltvPaths + '\'' +
                '}';
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong";
    }

    @GET
    @Path("getSchedule")
    @Produces(MediaType.APPLICATION_JSON)
    public Tv getSchedule() {
        return schedule;
    }

    public static void main(String[] args) throws Exception {
        final File cfg;
        if (args.length < 1 || !((cfg = new File(args[0])).exists())) {
            System.err.println("Usage: java -jar rcrdit.jar /path/to/rcrdit.cfg.xml");
            System.exit(1);
            return;
        }
        log.debug("rcrdit starting");

        final RcrdIt rcrdIt = new RcrdIt(cfg);

        final ApplicationPath ap = RcrdIt.class.getAnnotation(ApplicationPath.class);

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(ap == null ? rcrdIt.serverUri : rcrdIt.serverUri + ap.value() + "/"), rcrdIt, false);

        final File webapp = new File("./src/main/webapp/");
        final StaticHttpHandlerBase staticHttpHandler;

        if (new File(webapp, "index.html").canRead()) {
            //staticHttpHandler = new CLStaticHttpHandler(new URLClassLoader(new URL[]{webapp.toURI().toURL()}));
            staticHttpHandler = new StaticHttpHandler(webapp.getAbsolutePath());
            staticHttpHandler.setFileCacheEnabled(false); // don't cache files, because we are in development?
            System.out.println("File Caching disabled!");
        } else {
            staticHttpHandler = new CLStaticHttpHandler(RcrdIt.class.getClassLoader()); // jar class loader, leave cache enabled
        }

        server.getServerConfiguration().addHttpHandler(staticHttpHandler,
                rcrdIt.serverUri.replaceFirst("^[^/]+//[^/]+", "")
        );

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutdown requested");
            server.shutdownNow();
            rcrdIt.close();
            log.debug("shutdown complete, exiting...");
            System.exit(0);
        }));
        log.debug("rcrdit started");
    }
}
