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
package org.apache.jackrabbit.rmi.client.iterator;

import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;

/**
 * A ClientIterator for iterating remote versions.
 */
public class ClientVersionIterator extends ClientIterator
        implements VersionIterator {

    /** The current session. */
    private final Session session;

    /**
     * Creates a ClientVersionIterator instance.
     *
     * @param iterator      remote iterator
     * @param session       current session
     * @param factory       local adapter factory
     */
    public ClientVersionIterator(
            RemoteIterator iterator, Session session,
            LocalAdapterFactory factory) {
        super(iterator, factory);
        this.session = session;
    }

    /**
     * Creates and returns a local adapter for the given remote version.
     *
     * @param remote remote referecne
     * @return local adapter
     * @see ClientIterator#getObject(Object)
     */
    protected Object getObject(Object remote) {
        return getFactory().getVersion(session, (RemoteVersion) remote);
    }

    /**
     * Returns the next version in this iteration.
     *
     * @return next version
     * @see VersionIterator#nextVersion()
     */
    public Version nextVersion() {
        return (Version) next();
    }

}
