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
package org.apache.jackrabbit.webdav.jcr.lock;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jackrabbit.util.Text;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Maps between WebDAV lock tokens and JCR lock tokens.
 * <p>
 * The following notations are used:
 * 
 * <pre>
 * urn:uuid:<em>UUID of mapping</em>
 * opaquelocktoken:OPENSCOPED:<em>JCRLOCKTOKEN</em>
 * </pre>
 * 
 * The first format is used if the JCR lock does not reveal a lock token, such
 * as when it is a session-scoped lock (where SESSIONSCOPED is a constant UUID
 * defined below, and NODEIDENTIFIER is the suitably escaped JCR Node
 * identifier).
 * <p>
 * The second format is used for open-scoped locks (where OPENSCOPED is another
 * constant UUID defined below, and JCRLOCKTOKEN is the suitably escaped JCR
 * lock token).
 */
public class LockTokenMapper {

    private static final String OL = "opaquelocktoken:";

    private static final String OPENSCOPED = "dccce564-412e-11e1-b969-00059a3c7a00";

    private static final String SESSPREFIX = "urn:uuid:";
    private static final String OPENPREFIX = OL + OPENSCOPED + ":";

    // map node identifiers to randomized session locks, 4k mappings should be enough for everyone
    private static final Map<String, String> mappings = Collections.synchronizedMap(new LRUMap<>(4196));

    private LockTokenMapper() {
        /* This utility class should not be instantiated */
    }

    /**
     * Generate a WebDAV lock token from a JCR {@link Lock}.
     */
    public static String getDavLocktoken(Lock lock) throws RepositoryException {
        String jcrLockToken = lock.getLockToken();

        if (jcrLockToken == null) {
            return SESSPREFIX + Text.escape(getMapping(lock.getNode().getIdentifier()));
        } else {
            return OPENPREFIX + Text.escape(jcrLockToken);
        }
    }

    /**
     * Map from a WebDAV lock token to JCR lock token (requires an open scoped lock).
     */
    public static String getJcrLockToken(String token) throws RepositoryException {
        if (token.startsWith(OPENPREFIX)) {
            return Text.unescape(token.substring(OPENPREFIX.length()));
        } else {
            throw new RepositoryException("not a token for an open-scoped JCR lock: " + token);
        }
    }

    /**
     * Discards the DAV lock token mapping recorded for the given node, if
     * present. This is called when a lock is removed or newly created, so
     * that the token of an earlier lock on the same node can never be
     * reused against a later one.
     *
     * @param nodeIdentifier the identifier of the lock holding node.
     */
    public static void releaseSessionScopedToken(String nodeIdentifier) {
        mappings.remove(nodeIdentifier);
    }

    public static boolean isForSessionScopedLock(String token) {
        return token.startsWith(SESSPREFIX);
    }

    private static String getMapping(String nodeIdentifier) {
        return mappings.computeIfAbsent(
                nodeIdentifier,
                k -> UUID.randomUUID().toString()
        );
    }
}
