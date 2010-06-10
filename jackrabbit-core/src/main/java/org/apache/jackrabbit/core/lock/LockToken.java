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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.NodeId;

/**
 * Lock token
 */
class LockToken {

    /**
     * ID of node holding lock
     */
    private final NodeId id;

    /**
     * Create a new instance of this class. Used when creating new locks upon
     * a request.
     * @param id the id.
     */
    public LockToken(NodeId id) {
        this.id = id;
    }

    public NodeId getId() {
        return id;
    }

    /**
     * Parse a lock token string representation and return a lock token instance.
     * @param s string representation of lock token
     * @throws IllegalArgumentException if some field is illegal
     */
    public static LockToken parse(String s)
            throws IllegalArgumentException {

        int sep = s.lastIndexOf('-');
        if (sep == -1 || sep == s.length() - 1) {
            throw new IllegalArgumentException("Separator not found. Token [" + s + "]");
        }
        String uuid = s.substring(0, sep);
        if (getCheckDigit(uuid) != s.charAt(s.length() - 1)) {
            throw new IllegalArgumentException("Bad check digit. Token [" + s + "]");
        }
        return new LockToken(NodeId.valueOf(uuid));
    }

    /**
     * Return the string representation of a lock token
     * @return string representation
     * @see #toString
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(id.toString());
        buf.append('-');
        buf.append(getCheckDigit(id.toString()));
        return buf.toString();
    }

    /**
     * Return the check digit for a lock token, given by its UUID
     * @param uuid uuid
     * @return check digit
     */
    private static char getCheckDigit(String uuid) {
        int result = 0;

        int multiplier = 36;
        for (int i = 0; i < uuid.length(); i++) {
            char c = uuid.charAt(i);
            if (c >= '0' && c <= '9') {
                int num = c - '0';
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'A' && c <= 'F') {
                int num = c - 'A' + 10;
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'a' && c <= 'f') {
                int num = c - 'a' + 10;
                result += multiplier * num;
                multiplier--;
            }
        }

        int rem = result % 37;
        if (rem != 0) {
            rem = 37 - rem;
        }
        if (rem >= 0 && rem <= 9) {
            return (char) ('0' + rem);
        } else if (rem >= 10 && rem <= 35) {
            return (char) ('A' + rem - 10);
        } else {
            return '+';
        }
    }
}
