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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a synonym provider based on a properties file. Each line in the
 * properties file is treated as a synonym definition. Example:
 * <pre>
 * A=B
 * B=C
 * </pre>
 * This synonym provider will return B as a synonym for A and vice versa. The
 * same applies to B and C. However A is not considered a synonym for C, nor
 * C a synonym for A.
 */
public class PropertiesSynonymProvider implements SynonymProvider {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(PropertiesSynonymProvider.class);

    /**
     * An empty string array. Returned when no synonym is found.
     */
    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * Check at most every 10 seconds for configuration updates.
     */
    private static final long CHECK_INTERVAL = 10 * 1000;

    /**
     * The file system resource that contains the configuration.
     */
    private FileSystemResource config;

    /**
     * Timestamp when the configuration was checked last.
     */
    private long lastCheck;

    /**
     * Timestamp when the configuration was last modified.
     */
    private long configLastModified;

    /**
     * Contains the synonym mapping. Map&lt;String, String[]>
     */
    private Map<String, String[]> synonyms = new HashMap<String, String[]>();

    /**
     * {@inheritDoc}
     */
    public synchronized void initialize(FileSystemResource fsr)
            throws IOException {
        if (fsr == null) {
            throw new IOException("PropertiesSynonymProvider requires a path configuration");
        }
        try {
            config = fsr;
            synonyms = getSynonyms(config);
            configLastModified = config.lastModified();
            lastCheck = System.currentTimeMillis();
        } catch (FileSystemException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSynonyms(String term) {
        checkConfigUpdated();
        term = term.toLowerCase();
        String[] syns;
        synchronized (this) {
            syns = synonyms.get(term);
        }
        if (syns == null) {
            syns = EMPTY_ARRAY;
        }
        return syns;
    }

    //---------------------------------< internal >-----------------------------

    /**
     * Checks if the synonym properties file has been updated and this provider
     * should reload the synonyms. This method performs the actual check at most
     * every {@link #CHECK_INTERVAL}. If reloading fails an error is logged and
     * this provider will retry after {@link #CHECK_INTERVAL}.
     */
    private synchronized void checkConfigUpdated() {
        if (lastCheck + CHECK_INTERVAL > System.currentTimeMillis()) {
            return;
        }
        // check last modified
        try {
            if (configLastModified != config.lastModified()) {
                synonyms = getSynonyms(config);
                configLastModified = config.lastModified();
                log.info("Reloaded synonyms from {}", config.getPath());
            }
        } catch (Exception e) {
            log.error("Exception while reading synonyms", e);
        }
        // update lastCheck timestamp, even if error occurred (retry later)
        lastCheck = System.currentTimeMillis();
    }

    /**
     * Reads the synonym properties file and returns the contents as a synonym
     * Map.
     *
     * @param config the synonym properties file.
     * @return a Map containing the synonyms.
     * @throws IOException if an error occurs while reading from the file system
     *                     resource.
     */
    private static Map<String, String[]> getSynonyms(FileSystemResource config) throws IOException {
        try {
            Map<String, String[]> synonyms = new HashMap<String, String[]>();
            Properties props = new Properties();
            props.load(config.getInputStream());
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                addSynonym(key, value, synonyms);
                addSynonym(value, key, synonyms);
            }
            return synonyms;
        } catch (FileSystemException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * Adds a synonym definition to the map.
     *
     * @param term     the term
     * @param synonym  synonym for <code>term</code>.
     * @param synonyms the Map containing the synonyms.
     */
    private static void addSynonym(String term, String synonym, Map<String, String[]> synonyms) {
        term = term.toLowerCase();
        String[] syns = synonyms.get(term);
        if (syns == null) {
            syns = new String[]{synonym};
        } else {
            String[] tmp = new String[syns.length + 1];
            System.arraycopy(syns, 0, tmp, 0, syns.length);
            tmp[syns.length] = synonym;
            syns = tmp;
        }
        synonyms.put(term, syns);
    }
}
