/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.header.TimeoutHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.ActiveLock;

import java.io.IOException;

/**
 * <code>LockMethod</code>...
 */
public class LockMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(LockMethod.class);

    /**
     * Creates a new <code>LockMethod</code>.
     *
     * @param uri
     * @param lockScope
     * @param lockType
     * @param owner
     * @param timeout
     * @param isDeep
     */
    public LockMethod(String uri, Scope lockScope, Type lockType, String owner,
                      long timeout, boolean isDeep) throws IOException {
        this(uri, new LockInfo(lockScope, lockType, owner, timeout, isDeep));
    }

    /**
     * Creates a new <code>LockMethod</code>.
     *
     * @param uri
     * @param lockInfo
     */
    public LockMethod(String uri, LockInfo lockInfo) throws IOException {
        super(uri);
        if (lockInfo != null) {
            TimeoutHeader th = new TimeoutHeader(lockInfo.getTimeout());
            setRequestHeader(th);
            if (!lockInfo.isRefreshLock()) {
                DepthHeader dh = new DepthHeader(lockInfo.isDeep());
                setRequestHeader(dh);
                setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
                setRequestBody(lockInfo);
            }
        }
    }

    /**
     * Create a new 'Refresh' lock method.
     *
     * @param uri
     * @param timeout
     * @param lockTokens used to build the untagged If header.
     * @see IfHeader
     */
    public LockMethod(String uri, long timeout, String[] lockTokens) {
        super(uri);
        TimeoutHeader th = new TimeoutHeader(timeout);
        setRequestHeader(th);
        IfHeader ifh = new IfHeader(lockTokens);
        setRequestHeader(ifh);
    }

    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    public String getName() {
        return DavMethods.METHOD_LOCK;
    }

    public ActiveLock getResponseAsLock() throws IOException {
        checkUsed();
        // todo -> build lockdiscovery-prop -> retrieve activelock
        return null;
    }

    public String getLockToken() {
        checkUsed();
        CodedUrlHeader cuh = new CodedUrlHeader(DavConstants.HEADER_LOCK_TOKEN, getResponseHeader(DavConstants.HEADER_LOCK_TOKEN).getValue());
        return cuh.getCodedUrl();
    }
}