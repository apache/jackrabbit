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

/**
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class RepositoryConfiguration
{

    /**
     */
    private String configurationFile;

    /**
     */
    private String repositoryPath;

    /**
     */
    private String repositoryName;

    /**
     */
    private String workspaceName;

    /**
     */
    private boolean useJNDI;

    /**
     */
    private String jndiName;

    /** Creates a new instance of RepositoryConfiguration. */
    public RepositoryConfiguration()
    {
    }

    public String getConfigurationFile()
    {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile)
    {
        this.configurationFile = configurationFile;
    }

    public String getRepositoryPath()
    {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath)
    {
        this.repositoryPath = repositoryPath;
    }

    public boolean isUseJNDI()
    {
        return useJNDI;
    }

    public void setUseJNDI(boolean useJNDI)
    {
        this.useJNDI = useJNDI;
    }

    public String getJndiName()
    {
        return jndiName;
    }

    public void setJndiName(String jndiName)
    {
        this.jndiName = jndiName;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName)
    {
        this.repositoryName = repositoryName;
    }

    public String getWorkspaceName()
    {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName)
    {
        this.workspaceName = workspaceName;
    }
}
