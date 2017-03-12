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

package com.moparisthebest.rcrdit.autorec;

import com.moparisthebest.jdbc.Finishable;
import com.moparisthebest.rcrdit.xmltv.Program;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by mopar on 2/20/17.
 */
public class AutoRec implements Comparable<AutoRec>, Finishable {
    private Profile profile = null;
    private final int profileId, priority;
    private Pattern titlePattern;
    private final String title, channelName;
    private Set<DayOfWeek> daysOfWeek;
    private LocalTime betweenTimeStart, betweenTimeEnd;
    private Instant timeMin, timeMax;

    public AutoRec(final int profileId, final int priority, final String title, final String channelName, final Set<DayOfWeek> daysOfWeek, final LocalTime betweenTimeStart, final LocalTime betweenTimeEnd, final Instant timeMin, final Instant timeMax) {
        this.profileId = profileId;
        this.priority = priority;
        this.title = title;
        this.titlePattern = this.title == null ? null : Pattern.compile(this.title);
        this.channelName = channelName;
        this.daysOfWeek = daysOfWeek == null || daysOfWeek.isEmpty() ? null : daysOfWeek;
        this.betweenTimeStart = betweenTimeStart;
        this.betweenTimeEnd = betweenTimeEnd;
        this.timeMin = timeMin;
        this.timeMax = timeMax;
    }

    private AutoRec() {
        this(-1, -1, null, null, null, null, null, null, null);
    }

    @Override
    public void finish(final ResultSet rs) throws SQLException {
        this.titlePattern = this.title == null ? null : Pattern.compile(this.title);
    }

    public boolean matches(final Program program) {
        if (program == null)
            return false;
        // if the title doesn't match, no
        if (titlePattern != null && !titlePattern.matcher(program.getTitle()).matches())
            return false;
        // if we require a specific channel, and that doesn't match, no
        if (channelName != null && !channelName.equals(program.getChannelName()))
            return false;
        // if timeMin has not yet arrived, no
        if (timeMin != null && timeMin.isAfter(program.getStart()))
            return false;
        // if timeMax has passed, no
        if (timeMax != null && timeMax.isBefore(program.getStart()))
            return false;
        // if start time of day has not yet been reached, no
        if (betweenTimeStart != null && betweenTimeStart.isAfter(LocalTime.from(program.getStart().atZone(ZoneId.systemDefault()))))
            return false;
        // if end time of day has already passed, no
        if (betweenTimeEnd != null && betweenTimeEnd.isBefore(LocalTime.from(program.getStart().atZone(ZoneId.systemDefault()))))
            return false;
        // if day of week is not correct, no
        if (daysOfWeek != null && !daysOfWeek.contains(program.getStart().atZone(ZoneId.systemDefault()).getDayOfWeek()))
            return false;
        // everything matches, success!
        return true;
    }

    @Override
    public int compareTo(final AutoRec o) {
        // this looks reversed but we want highest priority first
        return Integer.compare(o.priority, this.priority);
    }

    public void setDaysOfWeek(final String daysOfWeek) {
        this.daysOfWeek = daysOfWeek == null || daysOfWeek.trim().isEmpty() ? null : Arrays.stream(daysOfWeek.split(",")).map(s -> DayOfWeek.of(Integer.parseInt(s))).collect(Collectors.toSet());
    }

    public void setBetweenTimeStart(final java.sql.Time betweenTimeStart) {
        this.betweenTimeStart = betweenTimeStart == null ? null : betweenTimeStart.toLocalTime();
    }

    public void setBetweenTimeEnd(final java.sql.Time betweenTimeEnd) {
        this.betweenTimeEnd = betweenTimeEnd == null ? null : betweenTimeEnd.toLocalTime();
    }

    public void setTimeMin(final Date timeMin) {
        this.timeMin = timeMin == null ? null : timeMin.toInstant();
    }

    public void setTimeMax(final Date timeMax) {
        this.timeMax = timeMax == null ? null : timeMax.toInstant();
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(final Profile profile) {
        this.profile = profile;
    }

    public int getProfileId() {
        return profileId;
    }

    public int getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        if(daysOfWeek == null)return null;
        return Collections.unmodifiableSet(daysOfWeek); // should never be called, might want to cache if called often
    }

    public String getChannelName() {
        return channelName;
    }

    public LocalTime getBetweenTimeStart() {
        return betweenTimeStart;
    }

    public LocalTime getBetweenTimeEnd() {
        return betweenTimeEnd;
    }

    public Instant getTimeMin() {
        return timeMin;
    }

    public Instant getTimeMax() {
        return timeMax;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoRec)) return false;
        final AutoRec autoRec = (AutoRec) o;
        return priority == autoRec.priority &&
                Objects.equals(profile, autoRec.profile) &&
                Objects.equals(title, autoRec.title) &&
                Objects.equals(channelName, autoRec.channelName) &&
                Objects.equals(daysOfWeek, autoRec.daysOfWeek) &&
                Objects.equals(betweenTimeStart, autoRec.betweenTimeStart) &&
                Objects.equals(betweenTimeEnd, autoRec.betweenTimeEnd) &&
                Objects.equals(timeMin, autoRec.timeMin) &&
                Objects.equals(timeMax, autoRec.timeMax);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, priority, title, channelName, daysOfWeek, betweenTimeStart, betweenTimeEnd, timeMin, timeMax);
    }

    @Override
    public String toString() {
        return "AutoRec{" +
                "profile=" + profile +
                ", priority=" + priority +
                ", titlePattern=" + titlePattern +
                ", title='" + title + '\'' +
                ", channelName='" + channelName + '\'' +
                ", daysOfWeek=" + daysOfWeek +
                ", betweenTimeStart=" + betweenTimeStart +
                ", betweenTimeEnd=" + betweenTimeEnd +
                ", timeMin=" + timeMin +
                ", timeMax=" + timeMax +
                '}';
    }
}
