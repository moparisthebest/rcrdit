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

import java.time.Instant;
import java.util.Objects;

/**
 * Created by mopar on 2/17/17.
 */
//@XmlRootElement(name="programme")
//@JsonIgnoreProperties(value={"previously-shown", "lang", "", "system", "date"})
//@JsonIgnoreProperties(ignoreUnknown = true)
public class Program {


    /*
        <programme start="20170218200000 -0500" stop="20170218203000 -0500" channelId="I92196.labs.zap2it.com">
		<title lang="en">Night Court</title>
		<sub-title lang="en">Clip Show</sub-title>
		<desc lang="en">An audit brings past recollections.</desc>
		<episode-num system="dd_progid">EP00003134.0129</episode-num>
		<previously-shown />
	</programme>
     */
    /*
    //@JacksonXmlProperty(localName = "start", isAttribute = true)
    public String start;
    //@JacksonXmlProperty(localName = "stop", isAttribute = true)
    public String stop;
    //@JacksonXmlProperty(localName = "channelId", isAttribute = true)
    public String channelId;
    //@JacksonXmlProperty(localName = "title")
    public String title;
    //@JacksonXmlProperty(localName = "sub-title")
    public String subTitle;
    //@JacksonXmlProperty(localName = "desc")
    public String desc;
    //@JacksonXmlProperty(localName = "episode-num")
    public String episodeNum;
    */
    // 20170217060000 -0500
    private final Instant start, stop;
    private final String channelId, channelName, title, subTitle, desc, episodeNum, date, category;
    private final boolean previouslyShown;

    public Program(final Instant start, final Instant stop, final String channelId, final String channelName, final String title, final String subTitle, final String desc, final String episodeNum, final String date, final String category, final boolean previouslyShown) {
        this.start = start;
        this.stop = stop;
        this.channelId = channelId;
        this.channelName = channelName;
        this.title = title;
        this.subTitle = subTitle;
        this.desc = desc;
        this.episodeNum = episodeNum;
        this.date = date;
        this.category = category;
        this.previouslyShown = previouslyShown;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getStop() {
        return stop;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getDesc() {
        return desc;
    }

    public String getEpisodeNum() {
        return episodeNum;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public boolean isPreviouslyShown() {
        return previouslyShown;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Program)) return false;
        final Program program = (Program) o;
        return previouslyShown == program.previouslyShown &&
                Objects.equals(start, program.start) &&
                Objects.equals(stop, program.stop) &&
                Objects.equals(channelId, program.channelId) &&
                Objects.equals(channelName, program.channelName) &&
                Objects.equals(title, program.title) &&
                Objects.equals(subTitle, program.subTitle) &&
                Objects.equals(desc, program.desc) &&
                Objects.equals(episodeNum, program.episodeNum) &&
                Objects.equals(date, program.date) &&
                Objects.equals(category, program.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, stop, channelId, channelName, title, subTitle, desc, episodeNum, date, category, previouslyShown);
    }

    @Override
    public String toString() {
        return "Program{" +
                "start=" + start +
                ", stop=" + stop +
                ", channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", title='" + title + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", desc='" + desc + '\'' +
                ", episodeNum='" + episodeNum + '\'' +
                ", date='" + date + '\'' +
                ", category='" + category + '\'' +
                ", previouslyShown=" + previouslyShown +
                '}';
    }
}
