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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;
import org.apache.jackrabbit.core.security.authorization.acl.ACLProvider;
import org.apache.jackrabbit.core.security.user.UserAccessControlProvider;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of the AccessControlProviderFactory
 */
public class AccessControlProviderFactoryImpl implements AccessControlProviderFactory {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(AccessControlProviderFactoryImpl.class);

    /**
     * The name of the security workspace (containing users...)
     */
    private String secWorkspaceName;
    private String defaultWorkspaceName;

    //---------------------------------------< AccessControlProviderFactory >---
    /**
     * @see AccessControlProviderFactory#init(Session)
     */
    public void init(Session securitySession) throws RepositoryException {
        secWorkspaceName = securitySession.getWorkspace().getName();
        if (securitySession instanceof SessionImpl) {
            defaultWorkspaceName = ((RepositoryImpl) securitySession.getRepository()).getConfig().getDefaultWorkspaceName();
        } // else: unable to determine default workspace name
    }

    /**
     * @see AccessControlProviderFactory#close()
     */
    public void close() throws RepositoryException {
        // nothing to do
    }

    /**
     * @see AccessControlProviderFactory#createProvider(Session, WorkspaceSecurityConfig)
     */
    public AccessControlProvider createProvider(Session systemSession, WorkspaceSecurityConfig config)
            throws RepositoryException {
        String workspaceName = systemSession.getWorkspace().getName();
        AccessControlProvider prov;
        Map props;
        if (config != null && config.getAccessControlProviderConfig() != null) {
            BeanConfig bc = config.getAccessControlProviderConfig();
            prov = bc.newInstance(AccessControlProvider.class);
            props = bc.getParameters();
        } else {
            log.debug("No ac-provider configuration for workspace " + workspaceName + " -> using defaults.");
            if (workspaceName.equals(secWorkspaceName) && !workspaceName.equals(defaultWorkspaceName)) {
                // UserAccessControlProvider is designed to work with an extra
                // workspace storing user and groups. therefore avoid returning
                // this ac provider for the default workspace.
                prov = new UserAccessControlProvider();
            } else {
                prov = new ACLProvider();
            }
            log.debug("Default provider for workspace " + workspaceName + " = " + prov.getClass().getName());
            props = Collections.emptyMap();
        }

        prov.init(systemSession, props);
        return prov;
    }
}
