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
package org.apache.jackrabbit.j2ee;

import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletConfig;

/**
 * The bootstrap configuration hold information about initial startup
 * parameters like repository config and home.
 *
 * It supports the following properties and init parameters:
 * <pre>
 * +-------------------+-------------------+
 * | Property Name     | Init-Param Name   |
 * +-------------------+-------------------+
 * | repository.home   | repository-home   |
 * | repository.config | repository-config |
 * | repository.name   | repository-name   |
 * +-------------------+-------------------+
 * </pre>
 */
public class BootstrapConfig extends AbstractConfig {

    private String repositoryHome;

    private String repositoryConfig;

    private String repositoryName;

    private JNDIConfig jndiConfig = new JNDIConfig(this);

    @Deprecated private RMIConfig rmiConfig = new RMIConfig(this);

    public void init(Properties props) throws ServletException {
        String property = props.getProperty("repository.home");;
        if (property != null) {
            setRepositoryHome(property);
        }
        property = props.getProperty("repository.config");;
        if (property != null) {
            setRepositoryConfig(property);
        }
        property = props.getProperty("repository.name");;
        if (property != null) {
            setRepositoryName(property);
        }
        jndiConfig.init(props);
        rmiConfig.init(props);
    }

    public void init(ServletConfig ctx) throws ServletException {
        String property = ctx.getInitParameter("repository-home");
        if (property != null) {
            setRepositoryHome(property);
        }
        property = ctx.getInitParameter("repository-config");
        if (property != null) {
            setRepositoryConfig(property);
        }
        property = ctx.getInitParameter("repository-name");
        if (property != null) {
            setRepositoryName(property);
        }
        jndiConfig.init(ctx);
        rmiConfig.init(ctx);
    }

    public String getRepositoryHome() {
        return repositoryHome;
    }

    public void setRepositoryHome(String repositoryHome) {

        this.repositoryHome = repositoryHome;
    }

    public String getRepositoryConfig() {
        return repositoryConfig;
    }

    public void setRepositoryConfig(String repositoryConfig) {
        this.repositoryConfig = repositoryConfig;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    /**
     * @deprecated RMI support is deprecated and will be removed in a future version of Jackrabbit; see <a href=https://issues.apache.org/jira/browse/JCR-4972 target=_blank>Jira ticket JCR-4972</a> for more information.
     */
    @Deprecated public JNDIConfig getJndiConfig() {
        return jndiConfig;
    }

    /**
     * @deprecated RMI support is deprecated and will be removed in a future version of Jackrabbit; see <a href=https://issues.apache.org/jira/browse/JCR-4972 target=_blank>Jira ticket JCR-4972</a> for more information.
     */
    @Deprecated public RMIConfig getRmiConfig() {
        return rmiConfig;
    }

    public void validate() {
        valid = repositoryName != null;
        jndiConfig.validate();
        rmiConfig.validate();
    }


    public void logInfos() {
        super.logInfos();
        if (jndiConfig.isValid()) {
            jndiConfig.logInfos();
        }
        if (rmiConfig.isValid()) {
            rmiConfig.logInfos();
        }
    }
}