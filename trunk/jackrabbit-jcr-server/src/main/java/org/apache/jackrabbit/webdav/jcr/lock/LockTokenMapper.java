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

import org.apache.jackrabbit.util.Text;

/**
 * Maps between WebDAV lock tokens and JCR lock tokens.
 * <p>
 * The following notations are used:
 * 
 * <pre>
 * opaquelocktoken:SESSIONSCOPED:<em>NODEIDENTIFIER</em>
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

    private static final String SESSIONSCOPED = "4403ef44-4124-11e1-b965-00059a3c7a00";
    private static final String OPENSCOPED = "dccce564-412e-11e1-b969-00059a3c7a00";

    private static final String SESSPREFIX = OL + SESSIONSCOPED + ":";
    private static final String OPENPREFIX = OL + OPENSCOPED + ":";

    public static String getDavLocktoken(Lock lock) throws RepositoryException {
        String jcrLockToken = lock.getLockToken();

        if (jcrLockToken == null) {
            return SESSPREFIX + Text.escape(lock.getNode().getIdentifier());
        } else {
            return OPENPREFIX + Text.escape(jcrLockToken);
        }
    }

    public static String getJcrLockToken(String token) throws RepositoryException {
        if (token.startsWith(OPENPREFIX)) {
            return Text.unescape(token.substring(OPENPREFIX.length()));
        } else {
            throw new RepositoryException("not a token for an open-scoped JCR lock: " + token);
        }
    }

    public static boolean isForSessionScopedLock(String token) {
        return token.startsWith(SESSPREFIX);
    }
}
