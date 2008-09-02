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
package org.apache.jackrabbit.rmi.server.jmx;

import javax.jcr.RepositoryException;

public interface JCRServerMBean {

    void start() throws Exception;

    void stop() throws Exception;

    /**
     * Creates a workspace in the managed repository.
     *
     * @param username administrator username
     * @param password administrator password
     * @param workspace name of the workspace to create
     * @throws RepositoryException if the workspace could not be created
     */
    void createWorkspace(String username, String password, String workspace)
        throws RepositoryException;

    String getLocalAddress();

    void setLocalAddress(String address);

    String getRemoteAddress();

    void setRemoteAddress(String address);

    String getRemoteEnvironment();

    void setRemoteEnvironment(String remoteEnvironment);

    String getLocalEnvironment();

    void setLocalEnvironment(String localEnvironment);

}
