/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav;

import javax.jcr.Session;

/**
 * <code>DavSession</code> wraps a {@link Session repository session}
 * object, that is obtained on
 * {@link javax.jcr.Repository#login(javax.jcr.Credentials, String) login} to
 * the underlaying repository.
 */
public interface DavSession {

    /**
     * Adds a reference to this <code>DavSession</code> indicating that
     * the underlaying {@link Session} object is needed for actions spanning over
     * multiple requests.
     *
     * @param reference to be added.
     */
    public void addReference(Object reference);

    /**
     * Releasing a reference to this <code>DavSession</code>. If no more
     * references are present, the underlaying {@link Session} may be discarded
     * (e.g by calling {@link Session#logout()}.
     *
     * @param reference to be removed.
     */
    public void removeReference(Object reference);

    /**
     * Unwrap the {@link Session repository session} object.
     *
     * @return the session object wrapped by this <code>DavSession</code>
     */
    public Session getRepositorySession();

    /**
     * Adds a lock token to this <code>DavSession</code>.
     *
     * @param token
     */
    public void addLockToken(String token);

    /**
     * Returns the lock tokens of this <code>DavSession</code>.
     *
     * @return
     */
    public String[] getLockTokens();

    /**
     * Removes a lock token from this <code>DavSession</code>.
     *
     * @param token
     */
    public void removeLockToken(String token);

}