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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.moparisthebest.jdbc.QueryMapper;
import com.moparisthebest.rcrdit.autorec.AutoRec;
import com.moparisthebest.rcrdit.autorec.Profile;
import com.moparisthebest.rcrdit.autorec.ProgramAutoRec;
import com.moparisthebest.rcrdit.scheduler.PartialScheduler;
import com.moparisthebest.rcrdit.scheduler.Scheduler;
import com.moparisthebest.rcrdit.scheduler.StartStop;
import com.moparisthebest.rcrdit.tuner.DummyTuner;
import com.moparisthebest.rcrdit.tuner.HDHomerun;
import com.moparisthebest.rcrdit.tuner.Tuner;
import com.moparisthebest.rcrdit.tuner.Tuners;
import com.moparisthebest.rcrdit.xmltv.Channel;
import com.moparisthebest.rcrdit.xmltv.Program;
import com.moparisthebest.rcrdit.xmltv.Tv;
import jersey.repackaged.com.google.common.collect.Lists;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import com.moparisthebest.rcrdit.requestbeans.GetScheduleRequest;
import com.moparisthebest.rcrdit.requestbeans.NewRecordingRequest;
import com.moparisthebest.rcrdit.responsebeans.GetScheduleResponse;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Created by mopar on 2/16/17.
 */
@ApplicationPath("rest")
@Path("")
public class RcrdIt extends ResourceConfig implements AutoCloseable {

    // for testing, should be null normally
    private static final LocalDateTime fakeTime = null;//LocalDateTime.of(2017, 10, 29, 14, 26);

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

    private final Scheduler scheduler = new PartialScheduler();

    private final String serverUri;


    public RcrdIt(final File cfg) throws Exception {
        register(this);
        register(JacksonFeature.class);
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
                case "Dummy":
                    tuners.add(new DummyTuner());
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
                                   final Program prog, final MessageDigest md, final boolean skipped) {
        addMeeting(calendar, tz, start, stop, new ProgramAutoRec(prog, prog.getAutoRec()), md, skipped);
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
        if(fakeTime != null)
            meeting.getProperties().add(new DtStamp(new DateTime(1489294362645L)));

        // add timezone info..
        meeting.getProperties().add(tz.getTimeZoneId());

        // generate unique identifier..
        md.reset();
        // incorporate everything to make this unique
        md.update(Long.toString(start.toEpochMilli()).getBytes(UTF_8));
        md.update(Long.toString(stop.toEpochMilli()).getBytes(UTF_8));
        md.update(Long.toString(prog.getProgram().getStart().toEpochMilli()).getBytes(UTF_8));
        md.update(Long.toString(prog.getProgram().getStop().toEpochMilli()).getBytes(UTF_8));
        md.update(prog.getProgram().getChannelName().getBytes(UTF_8));
        md.update(name.getBytes(UTF_8));
        md.update(description.getBytes(UTF_8));
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
        schedule.getPrograms().forEach(Program::clear);

        if (schedule.getPrograms().isEmpty() || autoRecs.isEmpty()) {
            log.debug("nothing to import.");
            return;
        }

        scheduler.schedulePrograms(autoRecs, schedule.getPrograms(), tuners.numTuners(), schedule.getLastEndTime(),
                RcrdIt.fakeTime != null ?
                        RcrdIt.fakeTime.toInstant(ZoneOffset.systemDefault().getRules().getOffset(RcrdIt.fakeTime)).truncatedTo(ChronoUnit.MINUTES) :
                        Instant.now().truncatedTo(ChronoUnit.MINUTES)
        );

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

        // set up startTimers and rcrdit.ics
        //for(final Program program : schedule.getPrograms()) {
        //for(int x = 0; x < schedule.getPrograms().size(); ++x) {
        for(int x = schedule.getPrograms().size() - 1; x >= 0; --x) {
            final Program program = schedule.getPrograms().get(x);
            if(program.getAutoRec() != null) {
                // wanted to record

                final List<StartStop> startStops = program.getStartStops();

                if(startStops.isEmpty()) {
                    // whole thing skipped...
                    addMeeting(calendar, tz, program.getStart(), program.getStop(), program, md, true);
                    continue;
                }
                if((startStops.size() % 2) != 0) { // todo: this is really bad, wtf
                    System.out.println(program.fullString());
                    continue;
                }
                int index = -1;
                final StartStop first = startStops.get(++index);
                final StartStop second = startStops.get(++index);
                if(startStops.size() == 2 && first.getInstant().equals(program.getStart()) && second.getInstant().equals(program.getStop())) {
                    // whole thing recorded...
                    Program toStop = first.getToStop();
                    addMeeting(calendar, tz, program.getStart(), program.getStop(), program, md, false);
                    RecordingTask rt = null;
                    if(toStop != null){
                        rt = new RecordingTask(toStop, program, program.getStart());
                    }else {
                        rt = new RecordingTask(program, program.getStart());
                    }
                    startTimers.add(rt);
                    continue;
                }
                if(first.isStart()) {
                    //Travis, uncomment me if I'm still needed
                    //if(first.getInstant().equals(program.getStart())) {
                        // not started at start time, skipped first part of program
                        //addMeeting(calendar, tz, program.getStart(), first.getInstant(), program, md, true);
                    //}
                    Program toStop = null;
                    Instant start = null, stop = null, lastStop = null;
                    for(final StartStop ss : program.getStartStops()) {
                        if(start == null && ss.isStart()) {
                            start = ss.getInstant();
                            toStop = ss.getToStop();
                        } else if(stop == null && !ss.isStart()) {
                            stop = ss.getInstant();
                        } else {
                            // both set
                            if(lastStop != null) { // todo: check if lastStop and start are the same, but shouldn't happen?
                                addMeeting(calendar, tz, lastStop, start, program, md, true);
                            }
                            addMeeting(calendar, tz, start, stop, program, md, false);

                            final RecordingTask rt = new RecordingTask(toStop, program, start);
                            startTimers.add(rt);

                            lastStop = stop;
                            start = stop = null;
                        }
                    }

                    if(lastStop != null && start != null) { // todo: check if lastStop and start are the same, but shouldn't happen?
                        addMeeting(calendar, tz, lastStop, start, program, md, true);
                    }
                    if(start != null && stop != null) {
                        addMeeting(calendar, tz, start, stop, program, md, false);

                        final RecordingTask rt = new RecordingTask(toStop, program, start);
                        startTimers.add(rt);
                    }

                } else {
                    log.error("holy shit should never happen, bad scheduler?");
                    throw new RuntimeException("holy shit should never happen, bad scheduler?");
                }
            }
        }
        
        log.debug("new import done.\n\n------\n\n");
        if(fakeTime != null) {
            try (FileOutputStream fout = new FileOutputStream("startTimers.txt")) {
                List<TimerTask> tmpTimers = Lists.reverse(startTimers);
                for (TimerTask tt : tmpTimers) {
                    fout.write(tt.toString().getBytes(UTF_8));
                    fout.write('\n');
                }
            } catch (Exception e) {
                // ignore e.printStackTrace();
            }
            try (FileOutputStream fout = new FileOutputStream("programs.txt")) {
                for (Program prog : schedule.getPrograms()) {
                    fout.write(prog.fullString().getBytes(UTF_8));
                    fout.write('\n');
                }
            } catch (Exception e) {
                // ignore e.printStackTrace();
            }
        }
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
                if(!skipped)
                    prog.addStartStop(new StartStop(prog.getStop(), false));
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
                // delete old autorecs
                final int autoRecsDeleted = qm.executeUpdate("DELETE FROM autorecs WHERE time_max < ?", java.sql.Timestamp.from(Instant.now()));
                log.debug("expired autorecs deleted: {}", autoRecsDeleted);
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
                schedule = Tv.readSchedule(xmltvPaths, allChannels, RcrdIt.fakeTime != null ? RcrdIt.fakeTime.withMinute(0) : LocalDateTime.now().withMinute(0));
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
        private final Instant runAt;

        RecordingTask(final ProgramAutoRec stop, final ProgramAutoRec start, final Instant runAt) {
            this.stop = stop;
            this.start = start;
            this.runAt = runAt;
            if(fakeTime == null)
                timer.schedule(this, Date.from(runAt));
        }

        RecordingTask(final ProgramAutoRec start, final Instant runAt) {
            this(null, start, runAt);
        }

        RecordingTask(final Program stop, final Program start, final Instant runAt) {
            this(stop == null ? null : new ProgramAutoRec(stop, stop.getAutoRec()), start == null ? null : new ProgramAutoRec(start, start.getAutoRec()), runAt);
        }

        RecordingTask(final Program start, final Instant runAt) {
            this(null, start, runAt);
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

        @Override
        public String toString() {
            return "RecordingTask{" +
                    "stop=" + stop +
                    ", start=" + start +
                    ", runAt=" + runAt +
                    "} ";
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
    
    @POST
    @Path("getRecordingProfiles")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Profile> getRecordingProfiles() {
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             QueryMapper qm = new QueryMapper(conn)) {
                final Map<Integer, Profile> profileMap = qm.toMap("SELECT profile_id, name, folder, run_at_recording_start, run_at_recording_finish FROM profile", new HashMap<>(), Integer.class, Profile.class);
                return profileMap;
        }catch(Exception e){
            log.error("Error in getSchedule",e);
        }
        return new HashMap<>();
    }
    
    @POST
    @Path("getAutoRecs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AutoRec> getAutoRecs() {
        return autoRecs;
    }
    
    @POST
    @Path("getCurrentlyRecording")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProgramAutoRec> getCurrentlyRecording() {
        List<ProgramAutoRec> currentlyRecording = new ArrayList<>();
        tuners.getTuners().stream().map((tuner) -> tuner.getRecording()).filter((rec) -> (rec != null)).forEach((rec) -> {
            currentlyRecording.add(rec);
        });
        return currentlyRecording;
    }
    
    @POST
    @Path("getUpcomingRecordings")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Long,Program> getUpcomingRecordings() {
        Map<Long,Program> returnMap = new LinkedHashMap<>();
        for(TimerTask t : startTimers){
            if(t instanceof RecordingTask){
                RecordingTask rt = (RecordingTask)t;
                if(rt.start != null && rt.scheduledExecutionTime() > System.currentTimeMillis()){
                    returnMap.put(rt.scheduledExecutionTime(),rt.start.getProgram());
                }
            }
        }
        return returnMap;
    }
    
    
    @POST
    @Path("getSchedule")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GetScheduleResponse getSchedule(GetScheduleRequest scheduleRequest) {
        List<Channel> channelList = new ArrayList<>();
        try{
            if(scheduleRequest.getEndTime().isAfter(GetScheduleRequest.getLastPossibleDateToDisplayOnSchedule(schedule.getLastEndTime()))){
                //not going to work, need to set things back a bit
                scheduleRequest.setStartAndEndTimeBasedOnLastDataAvailable(schedule.getLastEndTime());
            }
            int firstItemToLoad = ((scheduleRequest.getPageNum()-1) * scheduleRequest.getChannelsPerPage());
            if(schedule.getChannels().size() > firstItemToLoad){
                for(int i=firstItemToLoad; i<schedule.getChannels().size() && channelList.size() <scheduleRequest.getChannelsPerPage() ;i++){
                    channelList.add(new Channel(schedule.getChannels().get(i),scheduleRequest.getStartTime(),scheduleRequest.getEndTime()));
                }
            }
        }catch(Exception e){
            log.error("Error in getSchedule",e);
        }
        return new GetScheduleResponse(scheduleRequest,channelList);
    }
    
    @POST
    @Path("recordSingleInstanceOfProgram")
    @Consumes(MediaType.APPLICATION_JSON)
    public String recordSingleInstanceOfProgram(NewRecordingRequest recordingRequest) {
        
        try (Connection conn = DriverManager.getConnection(databaseUrl);
            QueryMapper qm = new QueryMapper(conn)) {
            String sql = "INSERT INTO autorecs (profile_id, priority, title, channel_name, days_of_week, between_time_start, between_time_end, time_min, time_max) "
                        + "VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)";
            java.sql.Timestamp startDate = null;
            java.sql.Timestamp endDate = null;
            if(recordingRequest.getStartDateEpochSeconds() != null)startDate = new Timestamp(recordingRequest.getStartDateEpochSeconds() * 1000);
            if(recordingRequest.getEndDateEpochSeconds() != null)endDate = new Timestamp(recordingRequest.getEndDateEpochSeconds() * 1000);
            String startTime = null;
            String endTime = null;
            if(recordingRequest.getStartTime() != null)startTime = recordingRequest.getStartTime().trim();
            if(recordingRequest.getStopTime()!= null)endTime = recordingRequest.getStopTime().trim();
            qm.executeUpdate(sql, recordingRequest.getProfileNo(),recordingRequest.getPriority(),recordingRequest.getTitle(),recordingRequest.getChannelName(),startTime,endTime,startDate,endDate);
            timer.schedule(new AutoRecTask(), 0);
        }catch(Exception e){
            log.error("Error in recordSingleInstanceOfProgram",e);
        }
        return "OK";
    }
    
    
    @POST
    @Path("refreshAutoRecs")
    public void refreshAutoRecs() {
        timer.schedule(new AutoRecTask(), 0);
    }
    
    public static void main(String[] args) throws Exception {
        //System.out.println(System.currentTimeMillis()); if(true) return;
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
