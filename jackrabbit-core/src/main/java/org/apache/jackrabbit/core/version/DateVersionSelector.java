/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.Calendar;

/**
 * This Class implements a version selector that selects a version by creation
 * date. The selected version is the latest that is older or equal than the
 * given date. If no version could be found <code>null</code> is returned
 * unless the <code>returnLatest</code> flag is set to <code>true</code>, where
 * the latest version is returned.
 * <xmp>
 * V1.0 - 02-Sep-2006
 * V1.1 - 03-Sep-2006
 * V1.2 - 05-Sep-2006
 *
 * new DateVersionSelector("03-Sep-2006").select() -> V1.1
 * new DateVersionSelector("04-Sep-2006").select() -> V1.1
 * new DateVersionSelector("01-Sep-2006").select() -> null
 * new DateVersionSelector("01-Sep-2006", true).select() -> V1.2
 * new DateVersionSelector(null, true).select() -> V1.2
 * </xmp>
 */
public class DateVersionSelector implements VersionSelector {

    /**
     * a version date hint
     */
    private Calendar date = null;

    /**
     * flag indicating that it should return the latest version, if no other
     * found
     */
    private boolean returnLatest = false;

    /**
     * Creates a <code>DateVersionSelector</code> that will select the latest
     * version of all those that are older than the given date.
     *
     * @param date
     */
    public DateVersionSelector(Calendar date) {
        this.date = date;
    }

    /**
     * Creates a <code>DateVersionSelector</code> that will select the latest
     * version of all those that are older than the given date.
     *
     * @param date
     * @param returnLatest
     */
    public DateVersionSelector(Calendar date, boolean returnLatest) {
        this.date = date;
        this.returnLatest = returnLatest;
    }

    /**
     * Returns the date hint
     *
     * @return the date hint.
     */
    public Calendar getDate() {
        return date;
    }

    /**
     * Sets the date hint
     *
     * @param date
     */
    public void setDate(Calendar date) {
        this.date = date;
    }

    /**
     * Returns the flag, if the latest version should be selected, if no
     * version can be found using the given hint.
     *
     * @return <code>true</code> if it returns latest.
     */
    public boolean isReturnLatest() {
        return returnLatest;
    }

    /**
     * Sets the flag, if the latest version should be selected, if no
     * version can be found using the given hint.
     *
     * @param returnLatest
     */
    public void setReturnLatest(boolean returnLatest) {
        this.returnLatest = returnLatest;
    }

    /**
     * Selects a version from the given version history using the previously
     * assigned hint in the following order: name, label, date, latest.
     *
     * @param versionHistory
     * @return
     * @throws RepositoryException
     */
    public Version select(VersionHistory versionHistory) throws RepositoryException {
        Version selected = null;
        if (date != null) {
            selected = DateVersionSelector.selectByDate(versionHistory, date);
        }
        if (selected == null && returnLatest) {
            selected = DateVersionSelector.selectByDate(versionHistory, null);
        }
        return selected;
    }

    /**
     * Selects a version by date.
     *
     * @param history
     * @param date
     * @return the latest version that is older than the given date date or
     * <code>null</code>
     * @throws RepositoryException
     */
    public static Version selectByDate(VersionHistory history, Calendar date)
            throws RepositoryException {
        long time = (date != null) ? date.getTimeInMillis() : Long.MAX_VALUE;
        long latestDate = Long.MIN_VALUE;
        Version latestVersion = null;
        VersionIterator iter = history.getAllVersions();
        while (iter.hasNext()) {
            Version v = iter.nextVersion();
            if (v.getPredecessors().length == 0) {
                // ignore root version
                continue;
            }
            long c = v.getCreated().getTimeInMillis();
            if (c > latestDate && c <= time) {
                latestDate = c;
                latestVersion = v;
            }
        }
        return latestVersion;
    }

    /**
     * returns debug information
     * @return debug information
     */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append("DateVersionSelector(");
        ret.append("date=");
        ret.append(date);
        ret.append(", returnLatest=");
        ret.append(returnLatest);
        ret.append(")");
        return ret.toString();
    }

}
