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
package org.apache.jackrabbit.spi;

/**
 * TODO: implement as bean and instanticate on client?
 * TODO: use credentials instead of UserID?
 * TODO: add name/value parameter facility
 * TODO: server returns set of name/value pairs which should be set by the client?
 *
 * <code>SessionInfo</code>...
 */
public interface SessionInfo {

    /**
     * 
     * @return
     */
    public String getUserID();

    /**
     *
     * @return
     */
    public String getWorkspaceName();

    /**
     *
     * @return
     */
    public String[] getLockTokens();

    /**
     *
     * @param lockToken
     */
    public void addLockToken(String lockToken);

    /**
     *
     * @param lockToken
     */
    public void removeLockToken(String lockToken);

    /**
     * Returns the identifier of the last {@link EventBundle} delivered using
     * this <code>SessionInfo</code>. When a <code>SessionInfo</code> is
     * initially aquired the returned event identifier is set to the last
     * <code>EventBundle</code> created by the SPI implementation previously to
     * the call to {@link RepositoryService#obtain(javax.jcr.Credentials, String)
     * RepositoryService.obtain()}. If there was no previous event <code>null</code>
     * is returned. Thus a <code>null</code> value will effectively return all
     * events that occurred since the start of the SPI server.
     * <p/>
     * For implementations, that do not support observation this method will
     * always return <code>null</code>.
     *
     * @return the identifier of the last {@link EventBundle} delivered using
     * this <code>SessionInfo</code>.
     */
    public String getLastEventBundleId();

    /**
     * Sets the identifier of the last {@link EventBundle} delivered using this
     * <code>SessionInfo</code>. This identifier will be used to retrieve the
     * subsequent event bundles when calling {@link RepositoryService#getEvents(SessionInfo, long, EventFilter[])}.
     *
     * @param eventBundleId the identifier of the last {@link EventBundle}
     *                      delivered using this <code>SessionInfo</code>.
     */
    public void setLastEventBundleId(String eventBundleId);
}