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

import java.io.File;

/**
 * Created by mopar on 2/20/17.
 */
public class Profile {
    private final int profileId;
    private File folder;
    private final String name, runAtRecordingStart, runAtRecordingFinish;

    public Profile(final int profileId, final File folder, final String name, final String runAtRecordingStart, final String runAtRecordingFinish) {
        this.profileId = profileId;
        this.folder = folder;
        this.name = name;
        this.runAtRecordingStart = runAtRecordingStart;
        this.runAtRecordingFinish = runAtRecordingFinish;
    }

    private Profile() {
        this(-1, null, null, null, null);
    }

    public int getProfileId() {
        return profileId;
    }

    public String getName() {
        return name;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(final String folder) {
        if (this.folder != null)
            throw new IllegalStateException("not allowed to modify folder");
        this.folder = new File(folder);
    }

    public String getRunAtRecordingStart() {
        return runAtRecordingStart;
    }

    public String getRunAtRecordingFinish() {
        return runAtRecordingFinish;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "profileId=" + profileId +
                ", folder=" + folder +
                ", name='" + name + '\'' +
                ", runAtRecordingStart='" + runAtRecordingStart + '\'' +
                ", runAtRecordingFinish='" + runAtRecordingFinish + '\'' +
                '}';
    }
}
