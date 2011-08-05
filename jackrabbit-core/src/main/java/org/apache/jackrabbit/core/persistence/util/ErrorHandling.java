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
package org.apache.jackrabbit.core.persistence.util;

import java.util.HashSet;

/**
 * ErrorHandling configuration abstraction class
 */
public final class ErrorHandling {

    /**
     * Controls if references to missing blob resources are treated as errors
     * or not.
     */
    public static final String IGNORE_MISSING_BLOBS = "IGN_MISSING_BLOBS";

    /** all available configuration codes */
    private static final String[] CODES = {
            IGNORE_MISSING_BLOBS
    };

    /** the flags */
    private final HashSet<String> flags = new HashSet<String>();

    /**
     * Creates a default error handling config.
     */
    public ErrorHandling() {
    }

    /**
     * Creates a new error handling configuration based on the given string.
     * The individual flags should be separated with "|".
     *
     * @param str flags
     */
    public ErrorHandling(String str) {
        for (int i = 0; i < CODES.length; i++) {
            if (str.indexOf(CODES[i]) >= 0) {
                flags.add(CODES[i]);
            }
        }
    }

    /**
     * Checks if error handling is set to ignore missing blobs
     * @return <code>true</code> if error handling is set to ignore missing blobs.
     */
    public boolean ignoreMissingBlobs() {
        return flags.contains(IGNORE_MISSING_BLOBS);
    }

    /**
     * Returns the string representation where the flags are separated
     * with "|".
     * @return the string representation.
     */
    public String toString() {
        StringBuilder ret = new StringBuilder("|");
        for (String flag : flags) {
            ret.append(flag);
        }
        ret.append("|");
        return ret.toString();
    }

}
