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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.transaction.xa.XAResource;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.XASessionInfo;

/**
 * <code>XASessionImpl</code> extends the regular session implementation with
 * access to the <code>XAResource</code>.
 */
public class XASessionImpl extends SessionImpl implements XASession {

    /**
     * The XASessionInfo of this <code>SessionImpl</code>.
     */
    private final XASessionInfo sessionInfo;

    /**
     * Creates a new <code>XASessionImpl</code>.
     *
     * @param repository the repository instance associated with this session.
     * @param sessionInfo the session info.
     * @param config the underlying repository configuration.
     * @throws RepositoryException if an error occurs while creating a session.
     */
    XASessionImpl(XASessionInfo sessionInfo, Repository repository,
                  RepositoryConfig config) throws RepositoryException {
        super(sessionInfo, repository, config);
        this.sessionInfo = sessionInfo;
    }

    //--------------------------------< XASession >-----------------------------

    /**
     * @see org.apache.jackrabbit.jcr2spi.XASession#getXAResource()
     */
    public XAResource getXAResource() {
        return sessionInfo.getXAResource();
    }

}
