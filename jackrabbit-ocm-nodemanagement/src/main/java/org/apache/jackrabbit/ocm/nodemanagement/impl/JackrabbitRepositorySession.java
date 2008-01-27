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
package org.apache.jackrabbit.ocm.nodemanagement.impl;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.nodemanagement.RepositorySession;

/** This class is the Jackrabbit JCR Repository session implementation.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class JackrabbitRepositorySession implements RepositorySession
{

    /** Creates a new instance of JackrabbitRepositorySession. */
    public JackrabbitRepositorySession()
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.RepositorySession#getSession
     */
    public Session getSession(String username, String password,
            RepositoryConfiguration configuration)
    {

        RepositoryUtil.registerRepository(configuration.getRepositoryName(),
                configuration.getConfigurationFile(),
                configuration.getRepositoryPath());

        Repository repository =
                RepositoryUtil.getRepository(configuration.getRepositoryName());

        Session session =  RepositoryUtil.login(repository, username, password);
        RepositoryUtil.setupSession(session);
        return session;
    }
}
