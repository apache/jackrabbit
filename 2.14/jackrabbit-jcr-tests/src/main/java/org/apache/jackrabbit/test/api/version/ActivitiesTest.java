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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;


/**
 * <code>ActivitiesTest</code> covers methods related to the Activities
 * feature in Versioning.
 * @since JCR 2.0
 */
public class ActivitiesTest extends AbstractVersionTest {

    private VersionManager vm;

    private static String PREFIX = "/jcr:system/jcr:activities/";
    
    protected void setUp() throws Exception {
        super.setUp();
        checkSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED);
        vm = superuser.getWorkspace().getVersionManager();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateRemoveActivity() throws Exception {
        
        Node an = null;
        
        try {
            an = vm.createActivity("foobar");
            assertNotNull(an);
            
            NodeType annt = an.getPrimaryNodeType();
            assertTrue("create node must be subtype of nt:activity", annt.isNodeType("nt:activity"));
        }
        finally {
            if (an != null) {
                vm.removeActivity(an);
            }
        }
    }

    public void testSetGetActivity() throws Exception {
        
        Node an = null;
        
        try {
            an = vm.createActivity("foobar");
            assertNotNull(an);
            
            assertNull(vm.getActivity());
            
            Node old = vm.setActivity(an);
            assertNull(old);
            assertEquals(an.getPath(), vm.getActivity().getPath());
            
            old = vm.setActivity(null);
            assertEquals(old.getPath(), an.getPath());
            assertNull(vm.getActivity());
        }
        finally {
            if (an != null) {
                vm.removeActivity(an);
            }
        }
    }

    public void testActivitiesPath() throws Exception {
        
        Node an = null;
        
        try {
            an = vm.createActivity("foobar");
            assertNotNull(an);
            
            NodeType annt = an.getPrimaryNodeType();
            assertTrue("create node must be subtype of nt:activity", annt.isNodeType("nt:activity"));

            assertTrue("path for activity must be below " + PREFIX + ", but was " + an.getPath(), an.getPath().startsWith(PREFIX));

            Node activities = superuser.getNode(PREFIX);

            try {
                activities.addNode("foobar");
                fail("/jcr:system/jcr:activities must be protected.");
            } catch (RepositoryException e) {
                // ok
            }
        }
        finally {
            if (an != null) {
                vm.removeActivity(an);
            }
        }
    }
    
    public void testActivitiesRelation() throws Exception {
        
        Node an = null;
        
        try {
            an = vm.createActivity("foobar");
            vm.setActivity(an);
            
            String path = versionableNode.getPath();
            
            if (versionableNode.isCheckedOut()) {
                vm.checkin(path);
            }
            
            vm.checkout(path);
            
            versionableNode = superuser.getNode(path);
            Property act = versionableNode.getProperty(Property.JCR_ACTIVITY);
            assertNotNull(act);
            assertEquals(PropertyType.REFERENCE, act.getType());
            assertTrue(act.getNode().isSame(an));
            
            versionableNode.remove();
            versionableNode.getSession().save();
        }
        finally {
            if (an != null) {
                vm.removeActivity(an);
            }
        }
    }

    public void testActivitiesRelationWithCheckpoint() throws Exception {

        Node an = null;

        try {
            an = vm.createActivity("foobar2");
            vm.setActivity(an);

            String path = versionableNode.getPath();

            vm.checkpoint(path);

            versionableNode = superuser.getNode(path);
            Property act = versionableNode.getProperty(Property.JCR_ACTIVITY);
            assertNotNull(act);
            assertEquals(PropertyType.REFERENCE, act.getType());
            assertTrue(act.getNode().isSame(an));

            versionableNode.remove();
            superuser.save();
        }
        finally {
            if (an != null) {
                vm.removeActivity(an);
            }
        }
    }
}

