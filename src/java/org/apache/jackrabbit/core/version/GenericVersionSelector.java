/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * This Class implements a generic version selector that either selects a
 * version by name, label or creation date. If no version is found and the
 * 'returnLatest' flag is set to <code>true</code>, the latest version is
 * returned.
 */
public class GenericVersionSelector implements VersionSelector {

    private String name = null;

    private String label = null;

    private Calendar date = null;

    private boolean returnLatest = true;

    public GenericVersionSelector() {
    }

    public GenericVersionSelector(String label) {
        this.label = label;
    }

    public GenericVersionSelector(Calendar date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public boolean isReturnLatest() {
        return returnLatest;
    }

    public void setReturnLatest(boolean returnLatest) {
        this.returnLatest = returnLatest;
    }

    public Version select(VersionHistory versionHistory) throws RepositoryException {
        Version selected = null;
        if (name != null) {
            selected = selectByName(versionHistory, name);
        }
        if (selected == null && label != null) {
            selected = selectByLabel(versionHistory, label);
        }
        if (selected == null && date != null) {
            selected = selectByDate(versionHistory, date);
        }
        if (selected == null && returnLatest) {
            selected = selectByDate(versionHistory, null);
        }
        return selected;
    }

    public static Version selectByName(VersionHistory history, String name)
            throws RepositoryException {
        return history.hasNode(name) ? history.getVersion(name) : null;
    }

    public static Version selectByLabel(VersionHistory history, String label)
            throws RepositoryException {
        return history.getVersionByLabel(label);
    }

    public static Version selectByDate(VersionHistory history, Calendar date)
            throws RepositoryException {
        long time = date == null ? Long.MAX_VALUE : date.getTimeInMillis();
        long latestDate = Long.MIN_VALUE;
        Version latestVersion = null;
        VersionIterator iter = history.getAllVersions();
        while (iter.hasNext()) {
            Version v = iter.nextVersion();
            long c = v.getCreated().getTimeInMillis();
            if (c > latestDate && c <= time) {
                latestDate = c;
                latestVersion = v;
            }
        }
        return latestVersion;
    }

}
