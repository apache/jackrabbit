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
package org.apache.jackrabbit.ocm.nodemanagement;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.nodemanagement.impl.RepositoryConfiguration;

/** Represents to JCR repository connection parameter.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public interface RepositorySession
{

    /** Connects to a JCR repository and returns a session to it.
     *
     * @param username Username to connect to repository
     * @param password Password
     * @param configuration Repository configuration
     * @return session
     */
    Session getSession(String username, String password,
            RepositoryConfiguration configuration);
}
