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
package org.apache.jackrabbit.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.retention.Hold;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.mem.MemoryFileSystem;
import org.apache.jackrabbit.core.retention.AbstractRetentionTest;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.retention.RetentionRegistryImpl;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RetentionEvaluatorImplTest</code>...
 */
public class RetentionRegistryImplTest extends AbstractRetentionTest {

    private static Logger log = LoggerFactory.getLogger(RetentionRegistryImplTest.class);

    private Node childN;
    private String childNPath;
    private Node childN2;
    private Node childN3;
    private Property childP;

    protected void setUp() throws Exception {
        super.setUp();

        if (!(superuser instanceof SessionImpl)) {
            throw new NotExecutableException();
        }

        childN = testRootNode.addNode(nodeName1);
        childNPath = childN.getPath();
        childN2 = testRootNode.addNode(nodeName2);
        childN3 = childN.addNode(nodeName3);

        Value v = getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE1, RepositoryStub.PROP_PROP_TYPE1, "test");
        childP = testRootNode.setProperty(propertyName1, v);
        testRootNode.save();

        retentionMgr.addHold(childNPath, getHoldName(), true);
        retentionMgr.setRetentionPolicy(childNPath, getApplicableRetentionPolicy("test"));
        
        superuser.save();
    }

    protected void tearDown() throws Exception {        
        try {
            Hold[] hs = retentionMgr.getHolds(childNPath);
            for (int i = 0; i < hs.length; i++) {
                retentionMgr.removeHold(childNPath, hs[i]);
            }
            superuser.save();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        try {
            if (retentionMgr.getRetentionPolicy(childNPath) != null) {
                retentionMgr.removeRetentionPolicy(childNPath);
            }
            superuser.save();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        super.tearDown();
    }

    private FileSystem createFileSystem() {
        FileSystem fs = new MemoryFileSystem();
        BufferedWriter writer = null;        
        try {
            fs.createFolder("/");
            FileSystemResource file = new FileSystemResource(fs, "/retention");

            writer = new BufferedWriter(new OutputStreamWriter(file.getOutputStream()));
            writer.write(((NodeImpl) childN).getNodeId().toString());
        } catch (FileSystemException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return fs;
    }
    
    public void testReadHoldFromFile() throws RepositoryException {
        PathResolver resolver = (SessionImpl) superuser;
        RetentionRegistryImpl re = new RetentionRegistryImpl((SessionImpl) superuser, createFileSystem());
        try {
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childN3.getPath()), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath + "/somechild"), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath + "/hold/is/deep"), false));

            assertFalse(re.hasEffectiveHold(resolver.getQPath(testNodePath), false));
            assertFalse(re.hasEffectiveHold(resolver.getQPath(childN2.getPath()), false));

        } finally {
            re.close();
        }
    }

    public void testReadRetentionFromFile() throws RepositoryException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistryImpl re = new RetentionRegistryImpl(s, createFileSystem());
        try {
            assertTrue(re.hasEffectiveRetention(s.getQPath(childNPath), false));
            assertTrue(re.hasEffectiveRetention(s.getQPath(childNPath + "/somechild"), true));

            assertFalse(re.hasEffectiveRetention(s.getQPath(testNodePath), false));
            assertFalse(re.hasEffectiveRetention(s.getQPath(childNPath + "/somechild"), false));
            assertFalse(re.hasEffectiveRetention(s.getQPath(childNPath + "/somechild/deepchild"), true));
            assertFalse(re.hasEffectiveRetention(s.getQPath(childP.getPath()), false));

        } finally {
            re.close();
            s.logout();
        }
    }

    public void testWriteFile() throws RepositoryException {
        PathResolver resolver = (SessionImpl) superuser;
        FileSystem fs = createFileSystem();
        RetentionRegistryImpl re = new RetentionRegistryImpl((SessionImpl) superuser, fs);

        try {
            // write the changes to the fs
            re.close();

            // create a new dummy registry again.
            re = new RetentionRegistryImpl((SessionImpl) superuser, fs);

            // test holds
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childN3.getPath()), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath + "/somechild"), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath + "/hold/is/deep"), false));

            // test policies
            assertTrue(re.hasEffectiveRetention(resolver.getQPath(childNPath), false));
            assertTrue(re.hasEffectiveRetention(resolver.getQPath(childNPath + "/somechild"), true));
        } finally {
            re.close();
        }
    }

    public void testWriteFileWithChanges() throws RepositoryException, NotExecutableException {
        PathResolver resolver = (SessionImpl) superuser;
        FileSystem fs = createFileSystem();
        RetentionRegistryImpl re = new RetentionRegistryImpl((SessionImpl) superuser, fs);
        String childN3Path = childN3.getPath();
        try {
            retentionMgr.removeRetentionPolicy(childNPath);
            retentionMgr.removeHold(childNPath, retentionMgr.getHolds(childNPath)[0]);
            superuser.save();
            
            retentionMgr.setRetentionPolicy(childN3Path, getApplicableRetentionPolicy("retentionOnChild2"));
            retentionMgr.addHold(childNPath, "holdOnChild", false);
            superuser.save();

            // write the changes to the fs
            re.close();

            // create a new dummy registry again.
            re = new RetentionRegistryImpl((SessionImpl) superuser, fs);

            // test holds
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath), false));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childNPath), true));
            assertTrue(re.hasEffectiveHold(resolver.getQPath(childN3Path), true));

            assertFalse(re.hasEffectiveHold(resolver.getQPath(childN3Path), false));
            assertFalse(re.hasEffectiveHold(resolver.getQPath(childN3Path + "/child"), false));
            assertFalse(re.hasEffectiveHold(resolver.getQPath(childN3Path + "/child"), true));

            // test policies
            assertTrue(re.hasEffectiveRetention(resolver.getQPath(childN3Path), false));
            assertTrue(re.hasEffectiveRetention(resolver.getQPath(childN3Path + "/child"), true));

            assertFalse(re.hasEffectiveRetention(resolver.getQPath(childN3Path + "/child/more"), true));
            assertFalse(re.hasEffectiveRetention(resolver.getQPath(childNPath), true));
            assertFalse(re.hasEffectiveRetention(resolver.getQPath(childNPath), false));
        } finally {
            re.close();
            // remove the extra policy that is not cleared upon teardown
            if (retentionMgr.getRetentionPolicy(childN3Path) != null) {
                retentionMgr.removeRetentionPolicy(childN3.getPath());
            }
            superuser.save();
        }
    }

    public void testRemoveHold() throws RepositoryException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        try {
            Hold[] holds = retentionMgr.getHolds(childNPath);
            for (int i = 0; i < holds.length; i++) {
                retentionMgr.removeHold(childNPath, holds[i]);
                // hold must still be in effect.
                assertTrue(re.hasEffectiveHold(s.getQPath(childNPath), false));
            }
            superuser.save();
            
            assertFalse(re.hasEffectiveHold(s.getQPath(childNPath), false));

        } finally {
            s.logout();
        }
    }

    public void testRemoveRetentionPolicy() throws RepositoryException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        try {
            retentionMgr.removeRetentionPolicy(childNPath);
            // retention must still be in effect.
            assertTrue(re.hasEffectiveRetention(s.getQPath(childNPath), false));

            superuser.save();

            assertFalse(re.hasEffectiveRetention(s.getQPath(childNPath), false));

        } finally {
            s.logout();
        }
    }

    public void testAddHold() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        Hold h = null;
        try {
            h = retentionMgr.addHold(childN2.getPath(), getHoldName(), false);
            // hold must not be effective yet
            assertFalse(re.hasEffectiveHold(s.getQPath(childN2.getPath()), false));

            superuser.save();
            assertTrue(re.hasEffectiveHold(s.getQPath(childN2.getPath()), false));

        } finally {
            s.logout();

            if (h != null) {
                retentionMgr.removeHold(childN2.getPath(), h);
                superuser.save();
            }
        }
    }

    public void testAddMultipleHold() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        try {
            retentionMgr.addHold(childN2.getPath(), getHoldName(), false);
            superuser.save();

            // hold is not deep -> only applies to childN2
            assertTrue(re.hasEffectiveHold(s.getQPath(childN2.getPath()), false));
            assertFalse(re.hasEffectiveHold(s.getQPath(childN2.getPath() + "/child"), false));

            retentionMgr.addHold(childN2.getPath(), getHoldName(), true);
            superuser.save();

            // now deep hold must be taken into account
            assertTrue(re.hasEffectiveHold(s.getQPath(childN2.getPath()), false));
            assertTrue(re.hasEffectiveHold(s.getQPath(childN2.getPath() + "/child"), false));

        } finally {
            s.logout();

            Hold[] hdls = retentionMgr.getHolds(childN2.getPath());
            for (int i = 0; i < hdls.length; i++) {
                retentionMgr.removeHold(childN2.getPath(), hdls[i]);
            }
            superuser.save();            
        }
    }

    public void testSetRetentionPolicy() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        try {
            retentionMgr.setRetentionPolicy(childN2.getPath(), getApplicableRetentionPolicy("test2"));
            // retention must not be effective yet
            assertFalse(re.hasEffectiveRetention(s.getQPath(childN2.getPath()), false));

            superuser.save();
            assertTrue(re.hasEffectiveRetention(s.getQPath(childN2.getPath()), false));

        } finally {
            s.logout();

            retentionMgr.removeRetentionPolicy(childN2.getPath());
            superuser.save();
        }
    }

    public void testChangeRetentionPolicy() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getSuperuserSession();
        RetentionRegistry re = s.getRetentionRegistry();
        try {
            retentionMgr.setRetentionPolicy(childN2.getPath(), getApplicableRetentionPolicy("test2"));
            superuser.save();
            retentionMgr.setRetentionPolicy(childN2.getPath(), getApplicableRetentionPolicy("test3"));
            superuser.save();

            assertTrue(re.hasEffectiveRetention(s.getQPath(childN2.getPath()), false));

        } finally {
            s.logout();

            retentionMgr.removeRetentionPolicy(childN2.getPath());
            superuser.save();
        }
    }
}