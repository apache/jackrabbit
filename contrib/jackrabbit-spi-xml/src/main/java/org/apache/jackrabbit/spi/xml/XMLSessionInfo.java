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
package org.apache.jackrabbit.spi.xml;

import org.apache.jackrabbit.spi.SessionInfo;

public class XMLSessionInfo implements SessionInfo {

    private final String user;

    public XMLSessionInfo(String user) {
        this.user = user;
    }

    //----------------------------------------------------------< SessionInfo>

    public String getWorkspaceName() {
        return "xml";
    }

    public String getUserID() {
        return user;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.SessionInfo#addLockToken(java.lang.String)
     */
    public void addLockToken(String lockToken) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.SessionInfo#getLastEventBundleId()
     */
    public String getLastEventBundleId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.SessionInfo#getLockTokens()
     */
    public String[] getLockTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.SessionInfo#removeLockToken(java.lang.String)
     */
    public void removeLockToken(String lockToken) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.SessionInfo#setLastEventBundleId(java.lang.String)
     */
    public void setLastEventBundleId(String eventBundleId) {
        // TODO Auto-generated method stub

    }

}
