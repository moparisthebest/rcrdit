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

import java.util.Comparator;

/**
 * Created by mopar on 2/18/17.
 */
public class ProgramTimeComparator implements Comparator<Program> {

    public static final Comparator<Program> instance = new ProgramTimeComparator();

    private ProgramTimeComparator() {
    }

    @Override
    public int compare(final Program o1, final Program o2) {
        int ret = o1.getStart().compareTo(o2.getStart());
        if (ret != 0)
            return ret;
        return o1.getStop().compareTo(o2.getStop());
    }
}
