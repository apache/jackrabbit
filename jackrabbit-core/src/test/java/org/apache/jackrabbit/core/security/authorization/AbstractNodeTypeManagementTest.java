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

import javax.jcr.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** <code>AbstractNodeTypeManagementTest</code>... */
public abstract class AbstractNodeTypeManagementTest extends AbstractEvaluationTest {

    private Node childNode;
    private String mixinName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Node child = testRootNode.addNode(nodeName2);
        if (child.isNodeType(mixReferenceable) || !child.canAddMixin(mixReferenceable)) {
            throw new NotExecutableException();
        }
        superuser.save();
        
        mixinName = getTestSession().getNamespacePrefix(NS_MIX_URI) + ":referenceable";
        childNode = getTestSession().getNode(child.getPath());
    }

    public void testCanAddMixin() throws RepositoryException, NotExecutableException {
        checkReadOnly(childNode.getPath());

        assertFalse(childNode.canAddMixin(mixinName));

        modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        assertTrue(childNode.canAddMixin(mixinName));

        modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, false);
        assertFalse(childNode.canAddMixin(mixinName));
    }

    public void testAddMixin() throws RepositoryException, NotExecutableException {
        checkReadOnly(childNode.getPath());

        try {
            childNode.addMixin(mixinName);
            childNode.save();
            fail("TestSession does not have sufficient privileges to add a mixin type.");
        } catch (AccessDeniedException e) {
            // success
        }

        modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        childNode.addMixin(mixinName);
        childNode.save();
    }

    public void testRemoveMixin() throws RepositoryException, NotExecutableException {
        ((Node) superuser.getItem(childNode.getPath())).addMixin(mixinName);
        superuser.save();

        checkReadOnly(childNode.getPath());

        try {
            childNode.removeMixin(mixinName);
            childNode.save();
            fail("TestSession does not have sufficient privileges to remove a mixin type.");
        } catch (AccessDeniedException e) {
            // success
        }

        modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        childNode.removeMixin(mixinName);
        childNode.save();
    }

    public void testSetPrimaryType() throws RepositoryException, NotExecutableException {
        Node child = (Node) superuser.getItem(childNode.getPath());
        String ntName = child.getPrimaryNodeType().getName();

        String changedNtName = "nt:folder";
        child.setPrimaryType(changedNtName);
        child.save();

        try {
            checkReadOnly(childNode.getPath());

            try {
                childNode.setPrimaryType(ntName);
                childNode.save();
                fail("TestSession does not have sufficient privileges to change the primary type.");
            } catch (AccessDeniedException e) {
                // success
                getTestSession().refresh(false); // TODO: see JCR-1916
            }

            modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
            childNode.setPrimaryType(ntName);
            childNode.save();

        } finally {
            if (!ntName.equals(child.getPrimaryNodeType().getName())) {
                child.setPrimaryType(ntName);
                child.save();
            }
        }
    }

    /**
     * Test difference between common jcr:write privilege an rep:write privilege
     * that includes the ability to set the primary node type upon child node
     * creation.
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testAddNode() throws RepositoryException, NotExecutableException {
        checkReadOnly(childNode.getPath());

        // with simple write privilege a child node can be added BUT no
        // node type must be specified.
        modifyPrivileges(childNode.getPath(), Privilege.JCR_WRITE, true);
        addChildNode(false);
        try {
            addChildNode(true);
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        }

        // adding jcr:nodeTypeManagement privilege will allow to use any
        // variant of Node.addNode.
        modifyPrivileges(childNode.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        addChildNode(false);
        addChildNode(true);
    }

    private void addChildNode(boolean specifyNodeType) throws RepositoryException {
        Node n = null;
        try {
            n = (specifyNodeType) ? childNode.addNode(nodeName3, testNodeType) : childNode.addNode(nodeName3);
        } finally {
            if (n != null) {
                n.remove();
                childNode.save();
            }
        }
    }

    public void testCopy() throws RepositoryException, NotExecutableException {
        Workspace wsp = getTestSession().getWorkspace();
        String parentPath = childNode.getParent().getPath();
        String srcPath = childNode.getPath();
        String destPath = parentPath + "/"+ nodeName3;

        checkReadOnly(parentPath);
        try {
            wsp.copy(srcPath, destPath);
            fail("Missing write privilege.");
        } catch (AccessDeniedException e) {
            // success
        }

        // with simple write privilege copying a node is not allowed.
        modifyPrivileges(parentPath, Privilege.JCR_WRITE, true);
        try {
            wsp.copy(srcPath, destPath);
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        }

        // adding jcr:nodeTypeManagement privilege will grant permission to copy.
        modifyPrivileges(parentPath, PrivilegeRegistry.REP_WRITE, true);
        wsp.copy(srcPath, destPath);
    }

    public void testWorkspaceMove() throws RepositoryException, NotExecutableException {
        Workspace wsp = getTestSession().getWorkspace();
        String parentPath = childNode.getParent().getPath();
        String srcPath = childNode.getPath();
        String destPath = parentPath + "/"+ nodeName3;

        checkReadOnly(parentPath);
        try {
            wsp.move(srcPath, destPath);
            fail("Missing write privilege.");
        } catch (AccessDeniedException e) {
            // success
        }

        // with simple write privilege moving a node is not allowed.
        modifyPrivileges(parentPath, Privilege.JCR_WRITE, true);
        try {
            wsp.move(srcPath, destPath);
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        }

        // adding jcr:nodeTypeManagement privilege will grant permission to move.
        modifyPrivileges(parentPath, PrivilegeRegistry.REP_WRITE, true);
        wsp.move(srcPath, destPath);
    }

    public void testSessionMove() throws RepositoryException, NotExecutableException {
        Session s = getTestSession();
        String parentPath = childNode.getParent().getPath();
        String srcPath = childNode.getPath();
        String destPath = parentPath + "/"+ nodeName3;

        checkReadOnly(parentPath);
        try {
            s.move(srcPath, destPath);
            s.save();
            fail("Missing write privilege.");
        } catch (AccessDeniedException e) {
            // success
        }

        // with simple write privilege moving a node is not allowed.
        modifyPrivileges(parentPath, Privilege.JCR_WRITE, true);
        try {
            s.move(srcPath, destPath);
            s.save();
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        }

        // adding jcr:nodeTypeManagement privilege will grant permission to move.
        modifyPrivileges(parentPath, PrivilegeRegistry.REP_WRITE, true);
        s.move(srcPath, destPath);
        s.save();
    }
       
    public void testSessionImportXML() throws RepositoryException, NotExecutableException, IOException {
        Session s = getTestSession();
        String parentPath = childNode.getPath();

        checkReadOnly(parentPath);
        try {
            s.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            s.save();
            fail("Missing write privilege.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            s.refresh(false);
        }

        // with simple write privilege moving a node is not allowed.
        modifyPrivileges(parentPath, Privilege.JCR_WRITE, true);
        try {
            s.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            s.save();
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            s.refresh(false);
        }

        // adding jcr:nodeTypeManagement privilege will grant permission to move.
        modifyPrivileges(parentPath, PrivilegeRegistry.REP_WRITE, true);
        s.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        s.save();
    }

    public void testWorkspaceImportXML() throws RepositoryException, NotExecutableException, IOException {
        Workspace wsp = getTestSession().getWorkspace();
        String parentPath = childNode.getPath();

        checkReadOnly(parentPath);
        try {
            wsp.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Missing write privilege.");
        } catch (AccessDeniedException e) {
            // success
        }

        // with simple write privilege moving a node is not allowed.
        modifyPrivileges(parentPath, Privilege.JCR_WRITE, true);
        try {
            wsp.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Missing privilege jcr:nodeTypeManagement.");
        } catch (AccessDeniedException e) {
            // success
        }

        // adding jcr:nodeTypeManagement privilege will grant permission to move.
        modifyPrivileges(parentPath, PrivilegeRegistry.REP_WRITE, true);
        wsp.importXML(parentPath, getXmlForImport(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    }

    /**
     * Simple XML for testing permissions upon import.
     * 
     * @return
     */
    private InputStream getXmlForImport() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"" +
                "         xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\"" +
                "         xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"" +
                "         sv:name=\"" + nodeName3 + "\">" +
                "    <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">" +
                "        <sv:value>" + testNodeType + "</sv:value>" +
                "    </sv:property>" +
                "</sv:node>";
        return new ByteArrayInputStream(xml.getBytes());
    }
}