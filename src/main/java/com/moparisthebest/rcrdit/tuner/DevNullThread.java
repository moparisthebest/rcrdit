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

import java.io.InputStream;

/**
 * Reads an input stream until EOF or IOException into nothingness
 * <p>
 * all threads share a buffer since it's only written to and never read from multithreaded access doesn't really matter
 * <p>
 * useful for ignoring streams from Process's
 */
public class DevNullThread extends Thread {
    private static final byte[] buffer = new byte[4096];

    private final InputStream is;

    DevNullThread(final InputStream is) {
        this.is = is;
    }

    @Override
    public void run() {
        try {
            while (is.read(buffer) != -1) {
                // do nothing
            }
        } catch (Throwable e) {
            // ignore, just exit we are done
        }
    }
}
