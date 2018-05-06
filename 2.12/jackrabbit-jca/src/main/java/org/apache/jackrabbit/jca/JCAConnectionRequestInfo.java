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
package org.apache.jackrabbit.jca;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.resource.spi.ConnectionRequestInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates the credentials for creating a
 * session from the repository.
 */
public final class JCAConnectionRequestInfo implements ConnectionRequestInfo {

    /**
     * Credentials.
     */
    private final Credentials creds;

    /**
     * Workspace.
     */
    private final String workspace;

    /**
     * Construct the request info.
     */
    public JCAConnectionRequestInfo(JCAConnectionRequestInfo cri) {
        this(cri.creds, cri.workspace);
    }

    /**
     * Construct the request info.
     */
    public JCAConnectionRequestInfo(Credentials creds, String workspace) {
        this.creds = creds;
        this.workspace = workspace;
    }

    /**
     * Return the workspace.
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Return the credentials.
     */
    public Credentials getCredentials() {
        return creds;
    }

    /**
     * Return the hash code.
     */
    public int hashCode() {
        int hash1 = workspace != null ? workspace.hashCode() : 0;
        int hash2 = creds != null ? computeCredsHashCode(creds) : 0;
        return hash1 ^ hash2;
    }

    /**
     * Return true if equals.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JCAConnectionRequestInfo) {
            return equals((JCAConnectionRequestInfo) o);
        } else {
            return false;
        }
    }

    /**
     * Return true if equals.
     */
    private boolean equals(JCAConnectionRequestInfo o) {
        return equals(workspace, o.workspace)
            && equals(creds, o.creds);
    }

    /**
     * Return true if equals.
     */
    private boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        } else {
            return false;
        }
    }

    /**
     * Return true if equals.
     */
    private boolean equals(char[] o1, char[] o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            return equals(new String(o1), new String(o2));
        } else {
            return false;
        }
    }

    /**
     * Return true if equals.
     */
    private boolean equals(Credentials o1, Credentials o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            if ((o1 instanceof SimpleCredentials) && (o2 instanceof SimpleCredentials)) {
                return equals((SimpleCredentials) o1, (SimpleCredentials) o2);
            } else {
                return o1.equals(o2);
            }
        } else {
            return false;
        }
    }

    /**
     * This method compares two simple credentials.
     */
    private boolean equals(SimpleCredentials o1, SimpleCredentials o2) {
        if (!equals(o1.getUserID(), o2.getUserID())) {
            return false;
        }

        if (!equals(o1.getPassword(), o2.getPassword())) {
            return false;
        }

        Map<String, Object> m1 = getAttributeMap(o1);
        Map<String, Object> m2 = getAttributeMap(o2);
        return m1.equals(m2);
    }

    /**
     * Return the credentials attributes.
     */
    private Map<String, Object> getAttributeMap(SimpleCredentials creds) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String[] keys = creds.getAttributeNames();

        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], creds.getAttribute(keys[i]));
        }

        return map;
    }

    /**
     * Returns Credentials instance hash code. Handles instances of
     * SimpleCredentials in a special way.
     */
    private int computeCredsHashCode(Credentials c) {
        if (c instanceof SimpleCredentials) {
            return computeSimpleCredsHashCode((SimpleCredentials) c);
        }
        return c.hashCode();
    }

    /**
     * Computes hash code of a SimpleCredentials instance. Ignores its own
     * hashCode() method because it's not overridden in SimpleCredentials.
     */
    private int computeSimpleCredsHashCode(SimpleCredentials c) {
        String userID = c.getUserID();
        char[] password = c.getPassword();
        Map<String, Object> m = getAttributeMap(c);
        final int prime = 31;
        int result = 1;
        result = prime * result + ((userID == null) ? 0 : userID.hashCode());
        for (int i = 0; i < password.length; i++) {
            result = prime * result + password[i];
        }
        result = prime * result + ((m == null) ? 0 : m.hashCode());
        return result;
    }
    
    @Override
    public String toString() {
        return "workspace (" + workspace + ") creds (" + creds + ")";
    }
}
