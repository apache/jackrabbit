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
package org.apache.jackrabbit.core.security.jsr283.security;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Repository;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractAccessControlTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractAccessControlTest.class);

    protected AccessControlManager acMgr;

    protected void setUp() throws Exception {
        super.setUp();

        acMgr = getAccessControlManager(superuser);
    }

    protected static AccessControlManager getAccessControlManager(Session s) throws RepositoryException, NotExecutableException {
        // TODO: fix (Replace by Session) test as soon as jackrabbit implements 283
        if (!(s instanceof SessionImpl)) {
            throw new NotExecutableException();
        }
        // TODO: uncomment again.
        // checkSupportedOption(Repository.OPTION_SIMPLE_ACCESS_CONTROL_SUPPORTED);
        try {
            return ((SessionImpl) s).getAccessControlManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }

    protected static void checkSupportedOption(Session s, String option) throws NotExecutableException {
        if (Boolean.FALSE.toString().equals(s.getRepository().getDescriptor(option))) {
            throw new NotExecutableException();
        }
    }

    protected void checkCanReadAc(String path) throws RepositoryException, NotExecutableException {
        if (!acMgr.hasPrivileges(path, new Privilege[] {getPrivilege(Privilege.READ_ACCESS_CONTROL)})) {
            throw new NotExecutableException();
        }
    }

    protected void checkCanModifyAc(String path) throws RepositoryException, NotExecutableException {
        if (!acMgr.hasPrivileges(path, new Privilege[] {getPrivilege(Privilege.MODIFY_ACCESS_CONTROL)})) {
            throw new NotExecutableException();
        }
    }

    protected String getPathToNonExistingNode() throws RepositoryException {
        String name = "nonexisting";
        String path = name;
        int i = 0;
        while (testRootNode.hasNode(path)) {
            path = name + i;
            i++;
        }

        path = testRootNode.getPath() + "/" + path;
        return path;
    }

    protected String getPathToProperty() throws RepositoryException {
        String path = testRootNode.getPath() + "/" + jcrPrimaryType;
        // TODO: remove cast to SessionImpl again once 283 is released.
        if (((SessionImpl) superuser).nodeExists(path)) {
            throw new RepositoryException("Path " + path + " should point to property.");
        }
        return path;
    }

    private Privilege getPrivilege(String name) throws NotExecutableException {
        try {
            Privilege[] supported = acMgr.getSupportedPrivileges(testRootNode.getPath());
            for (int i = 0; i < supported.length; i++) {
                if (supported[i].getName().equals(name)) {
                    return supported[i];
                }
            }
            throw new NotExecutableException("Unable to retrieve privilege with name "+ name);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Unable to retrieve privilege with name "+ name);
        }
    }
}