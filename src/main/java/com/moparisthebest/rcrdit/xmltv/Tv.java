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

package com.moparisthebest.rcrdit.xmltv;

import com.moparisthebest.sxf4j.impl.AbstractXmlElement;
import com.moparisthebest.sxf4j.impl.XmlElement;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

/**
 * Created by mopar on 2/17/17.
 */
//@JacksonXmlRootElement(localName = "tv")
//@JsonIgnoreProperties(ignoreUnknown = true)
public class Tv {
    //@JsonProperty("channel")
    //@JacksonXmlElementWrapper(localName = "channel", useWrapping = false)
    private final List<Channel> channels;

    //@JacksonXmlProperty(localName = "programme")
    //@JacksonXmlElementWrapper(useWrapping = false)
    private final List<Program> programs;

    private final Instant lastEndTime;

    public Tv(final List<Channel> channels, final List<Program> programs, final Instant lastEndTime) {
        this.channels = Collections.unmodifiableList(channels);
        this.programs = Collections.unmodifiableList(programs);
        this.lastEndTime = lastEndTime;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public List<Program> getPrograms() {
        return programs;
    }

    public Instant getLastEndTime() {
        return lastEndTime;
    }

    @Override
    public String toString() {
        return "Tv{" +
                "channels=" + channels +
                ", programs=" + programs +
                ", lastEndTime=" + lastEndTime +
                '}';
    }

    public static Tv readSchedule(final List<String> resources, final Set<String> allChannels, final LocalDateTime topOfHour) throws Exception {
        //try /*(InputStream is = new FileInputStream(resource))*/ {
            /*
            ObjectMapper mapper = new XmlMapper(new XmlFactory()); // create once, reuse
            //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            ObjectReader or = mapper.readerFor(Tv.class);
            return or.readValue(is);
            ObjectMapper objectMapper = new XmlMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            //final Object ret = objectMapper.readValue(is, Tv.class);
            final byte[] xml = Files.readAllBytes(new File(resource).toPath());
            System.out.println(new String(xml));
            final Object ret = objectMapper.readValue(xml, Tv.class);
            objectMapper.writeValue(new File("out.xml"), ret);
            */

        final DateTimeFormatter INSTANT = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2)
                .appendValue(HOUR_OF_DAY, 2)
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendValue(SECOND_OF_MINUTE, 2)
                .appendLiteral(' ')
                .appendOffset("+HHMM", "0000")
                .parseStrict()
                .toFormatter(Locale.US)
                .withZone(ZoneId.systemDefault())
                .withResolverStyle(ResolverStyle.STRICT);

        final List<Channel> channels = new ArrayList<>();
        final List<Program> programs = new ArrayList<>();
        Instant lastEndTime = Instant.now();

        for(final String resource : resources) {
            final XmlElement tv = AbstractXmlElement.getFactory().readFromFile(resource);
            //System.out.print(tv);

            final Map<String, Channel> chanIdToChannel = new HashMap<>();
            for (final XmlElement chan : tv.getChildren("channel")) {
                final Channel channel = new Channel(chan.getAttribute("id"), Arrays.stream(chan.getChildren("display-name")).map(XmlElement::getValue).collect(Collectors.toSet()));
                for (final String displayName : channel.getDisplayNames())
                    if (allChannels == null || allChannels.isEmpty() || allChannels.contains(displayName)) {
                        channel.displayName = displayName;
                        chanIdToChannel.put(channel.getId(), channel);
                        channels.add(channel);
                        break;
                    }
            }
            final Instant now = topOfHour.toInstant(ZoneOffset.systemDefault().getRules().getOffset(topOfHour)).truncatedTo(ChronoUnit.MINUTES);
            //final Instant now = Instant.now()..truncatedTo(ChronoUnit.MINUTES);
            for (final XmlElement prog : tv.getChildren("programme")) {
                final String chanId = prog.getAttribute("channel");
                final Channel channel = chanIdToChannel.get(chanId);
                if (channel != null) {
                    final Instant stop = INSTANT.parse(prog.getAttribute("stop"), Instant::from);
                    if (stop.isAfter(now)) {
                        final Program program = new Program(INSTANT.parse(prog.getAttribute("start"), Instant::from), stop, chanId, channel.getDisplayName(),
                                val(prog.getChild("title")), val(prog.getChild("sub-title")), val(prog.getChild("desc")), val(prog.getChild("episode-num")),
                                val(prog.getChild("date")), val(prog.getChild("category")), prog.getChild("previously-shown") != null
                        );
                        channel.programs.add(program);
                        programs.add(program);
                        if (stop.isAfter(lastEndTime))
                            lastEndTime = stop;
                    }
                }
            }
        }

        // de-duplicate, keep channels in order, programs don't matter since we will sort anyway...
        // in theory we should only have to do this if resources.size() > 1, but who knows what kind of crazy xml will appear...
        {
            final Set<Channel> dedup = new LinkedHashSet<>(channels);
            channels.clear();
            channels.addAll(dedup);
        }
        {
            final Set<Program> dedup = new HashSet<>(programs);
            programs.clear();
            programs.addAll(dedup);
        }

        programs.sort(ProgramTimeComparator.instance);
        channels.forEach(c -> c.programs.sort(ProgramTimeComparator.instance));

        return new Tv(channels, programs, lastEndTime);
            /*
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("menu: "+Arrays.toString(ret));
        return null;
        */
    }

    private static String val(final XmlElement x) {
        try {
            return x == null ? null : x.getValue();
        } catch (Exception e) {
            return null;
        }
    }
}
