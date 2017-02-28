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

import com.moparisthebest.rcrdit.xmltv.Program;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;

import static java.time.temporal.ChronoField.*;

/**
 * Created by mopar on 2/20/17.
 */
public class ProgramAutoRec implements Comparable<ProgramAutoRec> {
    private final Program program;
    private final AutoRec autoRec;

    public ProgramAutoRec(final Program program, final AutoRec autoRec) {
        Objects.requireNonNull(program, "program must be non-null");
        Objects.requireNonNull(autoRec, "autoRec must be non-null");
        this.program = program;
        this.autoRec = autoRec;
    }

    public Program getProgram() {
        return program;
    }

    public AutoRec getAutoRec() {
        return autoRec;
    }

    private static final char[] allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-.0123456789".toCharArray();

    private static String cleanFilename(final String name) {
        final StringBuilder sb = new StringBuilder(name.length());
        for (final char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append('-');
            } else {
                for (final char a : allowedChars)
                    if (c == a) {
                        sb.append(c);
                        break;
                    }
            }
        }
        return sb.toString();
    }

    private static String getName(final Program program, final String ext, final String extra, final int count) {
        final StringBuilder sb = new StringBuilder(program.getTitle().length() * 3);
        sb.append(program.getTitle());
        if (program.getDesc() != null)
            sb.append('-').append(program.getDesc());
        if (program.getEpisodeNum() != null)
            sb.append('-').append(program.getEpisodeNum());
        if (extra != null) {
            sb.append('-').append(extra);
            if (count > 0)
                sb.append('-').append(count);
        }
        // if no desc/num/extra append date so there will never be a Judge-Judy.ts
        if (program.getDesc() == null && program.getEpisodeNum() == null && extra == null)
            sb.append('-').append(fileUnique.format(program.getStart()));
        sb.append('.').append(ext);
        return sb.toString();
    }

    private static final DateTimeFormatter fileUnique = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('.')
            .appendValue(HOUR_OF_DAY, 2)
            .appendValue(MINUTE_OF_HOUR, 2)
            .parseStrict()
            .toFormatter(Locale.US)
            .withZone(ZoneId.systemDefault())
            .withResolverStyle(ResolverStyle.STRICT);

    public File getNewFileToWrite(final String ext) throws IOException {
        final File folder = new File(autoRec.getProfile().getFolder(), cleanFilename(program.getTitle()));
        folder.mkdirs();
        File file = new File(folder, cleanFilename(getName(program, ext, null, 0)));
        if (file.createNewFile())
            return file;
        int count = -1;
        // program.start in format 2017-02-20.1800
        final String startDate = fileUnique.format(program.getStart());
        do {
            file = new File(folder, cleanFilename(getName(program, ext, startDate, ++count)));
        } while (!file.createNewFile());
        return file;
    }

    @Override
    public int compareTo(final ProgramAutoRec o) {
        return this.autoRec.compareTo(o.autoRec);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramAutoRec)) return false;
        final ProgramAutoRec that = (ProgramAutoRec) o;
        return Objects.equals(program, that.program) &&
                Objects.equals(autoRec, that.autoRec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(program, autoRec);
    }

    @Override
    public String toString() {
        return "ProgramAutoRec{" +
                "program=" + program +
                ", autoRec=" + autoRec +
                '}';
    }
}
