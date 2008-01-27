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

import javax.jcr.Session;

/** Factory class to create a JCR repository session for different
 * JCR implementations.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class RepositorySessionFactory {

    public static final String JACKRABBIT = "jackrabbit";

    private static Session session;

    /**
     * Private constructor.
     */
    private RepositorySessionFactory()
    {
    }

    /** Returns a session to a JCR repository.
     *
     * @param jcrRepository
     * @param username Username to logon
     * @param password Password
     * @return session JCR repository session
     */
    public static Session getSession(String jcrRepository,
            String username, String password,
            RepositoryConfiguration configuration)
    {

        if (session == null)
        {
            if (jcrRepository != null)
            {
                if (jcrRepository.equals(JACKRABBIT))
                {
                    session = new JackrabbitRepositorySession().getSession(username, password, configuration);
                }

            }
        }

        return session;
    }
}
