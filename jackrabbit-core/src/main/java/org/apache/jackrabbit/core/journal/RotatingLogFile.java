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
package org.apache.jackrabbit.core.journal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a log file that can be rotated.
 */
public class RotatingLogFile implements Comparable<RotatingLogFile> {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(RotatingLogFile.class);

    /**
     * Log extension.
     */
    private static final String LOG_EXTENSION = "log";

    /**
     * Parent directory.
     */
    private final File directory;

    /**
     * Basename.
     */
    private final String basename;

    /**
     * Backing file.
     */
    private final File file;

    /**
     * Version number.
     */
    private int version;

    /**
     * Create a new instance of this class.
     *
     * @param file file itself
     * @throws IllegalArgumentException if the filename is malformed
     */
    private RotatingLogFile(File directory, String basename, File file)
            throws IllegalArgumentException {

        this.directory = directory;
        this.basename = basename;
        this.file = file;

        parseName();
    }

    /**
     * Parse the file name, ensuring that the file is actually made up
     * of the components we expect.
     *
     * @throws IllegalArgumentException if the name is malformed
     */
    private void parseName() throws IllegalArgumentException {
        String name = file.getName();
        int sep1 = name.indexOf('.');
        if (sep1 == -1) {
            throw new IllegalArgumentException("no dot in filename.");
        }
        if (!basename.equals(name.substring(0, sep1))) {
            throw new IllegalArgumentException("name does not start with " +
                    basename + ".");
        }
        int sep2 = name.indexOf('.', sep1 + 1);
        if (sep2 == -1) {
            sep2 = name.length();
        }
        if (!LOG_EXTENSION.equals(name.substring(sep1 + 1, sep2))) {
            throw new IllegalArgumentException("name does not contain " +
                    LOG_EXTENSION + ".");
        }
        if (sep2 < name.length()) {
            String versionS = name.substring(sep2 + 1);
            try {
                version = Integer.parseInt(versionS);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "extension is not a number: " + versionS);
            }
        }
    }

    /**
     * Return the backing file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Rotate this file.
     */
    public void rotate() {
        String newName = basename +
                "." + LOG_EXTENSION +
                "." + String.valueOf(version + 1);
        file.renameTo(new File(directory, newName));
    }

    /**
     * Compares this log file to another file. It will return
     * a negative number if this log file has a smaller version,
     * a positive number if this log file a bigger version
     * and <code>0</code> if they have the same version.
     */
    public int compareTo(RotatingLogFile o) {
        return version - o.version;
    }

    /**
     * List all log files inside some directory. The list returned is
     * guaranteed to be in descending order, i.e. it is safe to rotate
     * every file in turn without accidentally overwriting another one.
     *
     * @param directory parent directory
     * @param basename basename expected
     * @return array of log files found
     */
    public static RotatingLogFile[] listFiles(File directory, final String basename) {
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(basename + ".");
            }
        });

        ArrayList<RotatingLogFile> l = new ArrayList<RotatingLogFile>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                l.add(new RotatingLogFile(directory, basename, file));
            } catch (IllegalArgumentException e) {
                log.warn("Bogusly named journal file, skipped: " + files[i] +
                        ", reason: " + e.getMessage());
            }
        }
        RotatingLogFile[] logFiles = new RotatingLogFile[l.size()];
        l.toArray(logFiles);

        Arrays.sort(logFiles, new Comparator<RotatingLogFile>() {
            public int compare(RotatingLogFile o1, RotatingLogFile o2) {
                return o2.compareTo(o1);
            }
        });
        return logFiles;
    }
}
