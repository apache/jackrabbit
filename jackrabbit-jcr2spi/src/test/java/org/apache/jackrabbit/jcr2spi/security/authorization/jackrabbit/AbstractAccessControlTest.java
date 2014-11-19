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
package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.jcr2spi.SessionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAccessControlTest extends AbstractJCRTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractAccessControlTest.class);

    private AccessControlManager acm;
    private Set<String> toClear = new HashSet<String>();
    private Principal principal;
    private Principal secondPrincipal;
    
    // path to the node for which the policy is bind
    protected String path;
    private String childNPath;
    private String childNPath2;
    private String childPPath;
    private String childchildPPath;
    private String siblingPath;
    protected Node node;
    private NamePathResolver resolver;
    private SessionImpl sImpl;

    @Override
    protected void setUp() throws Exception, NotExecutableException {
       super.setUp();
       
        try {
            acm = getSession().getAccessControlManager();
            
            principal = new Principal() {
                public String getName() {
                    return "unknownPrincipal";
                }
            };
            
            secondPrincipal = new Principal() {
                public String getName() {
                    return "anotherUnknownPrincipal";
                }
            };
            // create some nodes below the test root in order to apply ac-stuff
            sImpl = (SessionImpl) getSession();
            node = testRootNode.addNode(nodeName1, testNodeType);
            Node cn1 = node.addNode(nodeName2, testNodeType);
            Property cp1 = node.setProperty(propertyName1, "anyValue");
            Node cn2 = node.addNode(nodeName3, testNodeType);

            Property ccp1 = cn1.setProperty(propertyName1, "childNodeProperty");

            Node n2 = testRootNode.addNode(nodeName2, testNodeType);
            getSession().save();

            path = node.getPath();
            childNPath = cn1.getPath();
            childNPath2 = cn2.getPath();
            childPPath = cp1.getPath();
            childchildPPath = ccp1.getPath();
            siblingPath = n2.getPath();
            resolver = sImpl.getNamePathResolver();

        } catch (RepositoryException e) {
            // do some clean up here...
            throw e;
        }
    }

    @Override
    public void tearDown() throws Exception {
        try {
            for (String path : toClear) {
                try {
                    AccessControlPolicy[] policies = acm.getPolicies(path);
                    for (AccessControlPolicy policy : policies)
                        acm.removePolicy(path, policy);
                    getSession().save();
                } catch (RepositoryException e) {
                    log.debug(e.getMessage());
                }
            }
            
            if (getSession() != null && getSession().isLive()) {
                getSession().logout();
            }            
        } finally {
            super.tearDown();
        }
    }
    
    protected AccessControlManager getACManager() {
        return acm;
    }
    protected Set<String> toClear() {
        return toClear;
    }
    
    protected Session getSession() {
        return superuser;
    }
    
    protected Principal getUnknownPrincipal() {
        return principal;
    }
    
    protected Principal getAnotherUnknownPrincipal() {
        return secondPrincipal;
    }
    
    protected void checkCanReadAc(String path) throws RepositoryException, NotExecutableException {
        if (!acm.hasPrivileges(path, privilegesFromName(Privilege.JCR_READ_ACCESS_CONTROL))) {
            throw new RepositoryException();
        }
    }

    protected void checkCanModify(String path) throws RepositoryException,
            NotExecutableException {
        if (!acm.hasPrivileges(path, privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL))) {
            throw new RepositoryException();
        }
    }

    protected Privilege[] privilegesFromNames(String[] privilegeNames)
            throws RepositoryException {
        Privilege[] privs = new Privilege[privilegeNames.length];
        for (int i = 0; i < privilegeNames.length; i++) {
            privs[i] = acm.privilegeFromName(privilegeNames[i]);
        }
        return privs;
    }

    protected Privilege[] privilegesFromName(String privilegeName) throws RepositoryException {
        return new Privilege[] { acm.privilegeFromName(privilegeName) };
    }

    protected Privilege[] getPrivileges(int i) throws RepositoryException {
        Privilege[][] privilegePool = new Privilege[][] {
                privilegesFromName(Privilege.JCR_READ),
                privilegesFromName(Privilege.JCR_ADD_CHILD_NODES),
                privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL),
                privilegesFromName(Privilege.JCR_NODE_TYPE_MANAGEMENT),
                privilegesFromName(Privilege.JCR_LIFECYCLE_MANAGEMENT)
        };
        return privilegePool[i];
    }

    protected void checkReadOnly(String path) throws RepositoryException, NotExecutableException {
        Privilege[] privs = getACManager().getPrivileges(path);
        assertTrue(privs.length == 1);
        assertEquals(privilegesFromName(Privilege.JCR_READ)[0], privs[0]);
    }
}
