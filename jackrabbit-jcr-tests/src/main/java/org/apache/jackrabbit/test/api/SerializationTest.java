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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * <code>SerializationTest</code> contains the test cases for the method
 * <code>Workspace.exportSysView()</code> and <code>Session.importSysView()</code>.
 * <p/>
 * This class exports and re-imports the repository. The tests check for
 * differences between the original and the re-imported repository.
 *
 * @test
 * @sources SerializationTest.java
 * @executeClass org.apache.jackrabbit.test.api.SerializationTest
 * @keywords level2
 */
public class SerializationTest extends AbstractJCRTest {
    protected Workspace workspace;
    protected File file;
    protected TreeComparator treeComparator;

    protected final boolean CONTENTHANDLER = true, STREAM = false;
    protected final boolean WORKSPACE = true, SESSION = false;
    protected final boolean SKIPBINARY = true, SAVEBINARY = false;
    protected final boolean NORECURSE = true, RECURSE = false;

    protected Session session;

    public void setUp() throws RepositoryException, Exception {
        super.setUp();

        try {
            session = superuser;
            workspace = session.getWorkspace();
            file = File.createTempFile("serializationTest", ".xml");
            log.print("Tempfile: " + file.getAbsolutePath());
  
            SerializationContext sc = new SerializationContext(this);
            treeComparator = new TreeComparator(sc, session);
            treeComparator.createComplexTree(treeComparator.WORKSPACE);
        }
        catch (Exception ex) {
            if (file != null) {
                file.delete();
                file = null;
            }
            throw (ex);
        }
    }

    public void tearDown() throws Exception {
        if (file != null) {
            file.delete();
            file = null;
        }
        if (session != null && session.isLive()) {
            session.logout();
            session = null;
        }
        if (treeComparator != null) {
            treeComparator.tearDown();
        }
        workspace = null;
        super.tearDown();
    }

// ---------------< versioning exception tests >-----------------------------------------
    /**
     * Creates a simple target tree consisting of a checked-in node and an
     * ordinary child node below. Throws a {@link NotExecutableException} if
     * {@link #testNodeType} is not versionable.
     *
     * @param returnParent Whether the method returns a checked-in parent or the
     *                     child of a checked-in parent.
     * @return The requested node (a node that is checked in or that has a
     *         parent that is checked in).
     */
    protected Node initVersioningException(boolean returnParent) throws RepositoryException, NotExecutableException, IOException {
        Node vNode = testRootNode.addNode(nodeName1, testNodeType);
        if (!vNode.isNodeType(mixVersionable)) {
            if (vNode.canAddMixin(mixVersionable)) {
                vNode.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("NodeType: " + testNodeType + " is not versionable");
            }
        }
        Node vChild = vNode.addNode(nodeName2, testNodeType);
        session.save();
        vNode.checkin();

        exportRepository(SKIPBINARY, RECURSE);

        if (returnParent) {
            return vNode;
        } else {
            return vChild;
        }
    }

    public void doTestVersioningExceptionFileParent(boolean useWorkspace, boolean useHandler)
            throws Exception {
        Node n = initVersioningException(true);

        FileInputStream in = new FileInputStream(file);
        try {
            doImport(n.getPath(), in, useWorkspace, useHandler);
            fail("Importing to a checked-in node must throw a ConstraintViolationException.");
        } catch (VersionException e) {
            // success
        } finally {
            in.close();
        }
    }

    public void doTestVersioningExceptionFileChild(boolean useWorkspace, boolean useHandler)
            throws Exception {
        Node n = initVersioningException(false);

        FileInputStream in = new FileInputStream(file);
        try {
            doImport(n.getPath(), in, useWorkspace, useHandler);
            fail("Importing to a child of a checked-in node must throw a ConstraintViolationException.");
        } catch (VersionException e) {
            // success
        } finally {
            in.close();
        }
    }

    public void testVersioningExceptionFileParentWorkspaceContentHandler() throws Exception {
        doTestVersioningExceptionFileParent(WORKSPACE, CONTENTHANDLER);
    }

    public void testVersioningExceptionFileParentSessionContentHandler() throws Exception {
        doTestVersioningExceptionFileParent(SESSION, CONTENTHANDLER);
    }

    public void testVersioningExceptionFileParentWorkspace() throws Exception {
        doTestVersioningExceptionFileParent(WORKSPACE, STREAM);
    }

    public void testVersioningExceptionFileParentSession() throws Exception {
        doTestVersioningExceptionFileParent(SESSION, STREAM);
    }

    public void testVersioningExceptionFileChildWorkspaceContentHandler() throws Exception {
        doTestVersioningExceptionFileChild(WORKSPACE, CONTENTHANDLER);
    }

    public void testVersioningExceptionFileChildSessionContentHandler() throws Exception {
        doTestVersioningExceptionFileChild(SESSION, CONTENTHANDLER);
    }

    public void testVersioningExceptionFileChildWorkspace() throws Exception {
        doTestVersioningExceptionFileChild(WORKSPACE, STREAM);
    }

    public void testVersioningExceptionFileChildSession() throws Exception {
        doTestVersioningExceptionFileChild(SESSION, STREAM);
    }

// ----------------< locking exceptions tests >----------------------------
    /**
     * Tests whether importing a tree respects locking.
     */
    public void doTestLockException(boolean useWorkspace, boolean useHandler)
            throws Exception {
        exportRepository(SKIPBINARY, RECURSE);
        if (isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            //A LockException is thrown if a lock prevents the addition of the subtree.
            Node lNode = testRootNode.addNode(nodeName1);
            lNode.addMixin(mixLockable);
            testRootNode.save();
            Lock lock = lNode.lock(true, true);
            session.removeLockToken(lock.getLockToken());   //remove the token, so the lock is for me, too
            FileInputStream in = new FileInputStream(file);
            try {
                doImport(lNode.getPath(), in, useWorkspace, useHandler);
            } catch (LockException e) {
                // success
            } finally {
                in.close();
            }
        } else {
            log.println("Locking not supported.");
        }
    }

    public void testLockExceptionWorkspaceWithHandler() throws Exception {
        doTestVersioningExceptionFileChild(WORKSPACE, CONTENTHANDLER);
    }

    public void testLockExceptionSessionWithHandler() throws Exception {
        doTestVersioningExceptionFileChild(SESSION, CONTENTHANDLER);
    }

    public void testLockExceptionWorkspace() throws Exception {
        doTestVersioningExceptionFileChild(WORKSPACE, STREAM);
    }

    public void testLockExceptionSession() throws Exception {
        doTestVersioningExceptionFileChild(SESSION, STREAM);
    }

//--------------< Import of invalid xml file tests >-------------------------------------------
    /**
     * Tests whether importing an invalid XML file throws a SAX exception. The
     * file used here is more or less garbage.
     */
    public void testInvalidXmlThrowsSaxException()
            throws IOException, ParserConfigurationException {
        StringReader in = new StringReader("<this is not a <valid> <xml> file/>");
        ContentHandler ih = null;
        try {
            ih = session.getImportContentHandler(treeComparator.targetFolder,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (RepositoryException e) {
            fail("ImportHandler not created: " + e);
        }
        helpTestSaxException(ih, in, "session");

        in = new StringReader("<this is not a <valid> <xml> file/>");
        try {
            ih = workspace.getImportContentHandler(treeComparator.targetFolder, 0);
        } catch (RepositoryException e) {
            fail("ImportHandler not created: " + e);
        }
        helpTestSaxException(ih, in, "workspace");
    }

    /**
     * Helper method for testSaxException.
     *
     * @param ih
     * @param in
     */
    private void helpTestSaxException(ContentHandler ih, Reader in, String mode)
            throws IOException {
        try {
            createXMLReader(ih).parse(new InputSource(in));
            fail("Parsing an invalid XML file with via " + mode + " ContentHandler did not throw a SAXException.");
        } catch (SAXException e) {
            // success
        }
    }

    /**
     * Tests whether importing an invalid XML file throws a InvalidSerializedDataException.
     * The file used here is more or less garbage.
     */
    public void testInvalidXmlThrowsInvalidSerializedDataException()
            throws RepositoryException, IOException {

        String data = "<this is not a <valid> <xml> file/>";
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());

        try {
            session.importXML(treeComparator.targetFolder, in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Importing a invalid XML file should throw a InvalidSerializedDataException.");
        } catch (InvalidSerializedDataException e) {
            // ok
        }
        in = new ByteArrayInputStream(data.getBytes());
        try {
            workspace.importXML(treeComparator.targetFolder, in, 0);
            fail("Importing a invalid XML file should throw a InvalidSerializedDataException.");
        } catch (InvalidSerializedDataException e) {
            // ok
        }
    }

// -------------------< PathNotFoundException tests >------------------------------------
    /**
     * Supplying an invalid repository path for import must throw a
     * PathNotFoundException
     */
    public void testWorkspaceGetImportContentHandlerExceptions() throws RepositoryException {
        //Specifying a path that does not exist throws a PathNotFound exception
        try {
            workspace.getImportContentHandler(treeComparator.targetFolder + "/thisIsNotAnExistingNode",
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Specifying a non-existing path must throw a PathNotFoudException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    /**
     * Supplying an invalid repository path for import must throw a
     * PathNotFoundException
     */
    public void testSessionGetImportContentHandlerExceptions() throws RepositoryException {
        //Specifying a path that does not exist throws a PathNotFound exception
        try {
            session.getImportContentHandler(treeComparator.targetFolder + "/thisIsNotAnExistingNode",
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Specifying a non-existing path must throw a PathNotFoudException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    /**
     * Tests the exception when importing: If the parent node does not exist.
     */
    public void testSessionImportXmlExceptions() throws RepositoryException, IOException {
        exportRepository(SKIPBINARY, RECURSE);
        FileInputStream in = new FileInputStream(file);

        // If no node exists at parentAbsPath, a PathNotFoundException is thrown.
        try {
            session.importXML(treeComparator.targetFolder + "/thisNodeDoesNotExist",
                    in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("Importing to a non-existing node does not throw a PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        } finally {
            in.close();
        }
    }

    /**
     * Tests the exceptions when importing: If the parent node does not exist,
     * and if an IO error occurs.
     */
    public void testWorkspaceImportXmlExceptions() throws RepositoryException, IOException {
        exportRepository(SKIPBINARY, RECURSE);
        FileInputStream in = new FileInputStream(file);

        //If no node exists at parentAbsPath, a PathNotFoundException is thrown.
        try {
            workspace.importXML(treeComparator.targetFolder + "/thisNodeDoesNotExist", in, 0);
            fail("Importing to a non-existing node does not throw a PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        } finally {
            in.close();
        }
    }

// ---------------< Overwrite existing target node tests     >------------------------------
// ---------------< in case same name siblings not supported >------------------------------
    /**
     * Tries to overwrite an existing node. This only works for nodes that do
     * not allow same-name siblings.
     */
    public void doTestOverwriteException(boolean useWorkspace, boolean useHandler)
            throws Exception {
        //If deserialization would overwrite an existing item,
        // an ItemExistsException respective a SAXException is thrown.

        Node folder = testRootNode.addNode("myFolder", treeComparator.sc.sameNameSibsFalseChildNodeDefinition);
        Node subfolder = folder.addNode("subfolder");

        session.save();
        FileOutputStream out = new FileOutputStream(file);
        try {
            session.exportSystemView(subfolder.getPath(), out, true, true);
        } finally {
            out.close();
        }

        FileInputStream in = new FileInputStream(file);
        try {
            if (useHandler) {
                try {
                    doImport(folder.getPath(), in, useWorkspace, useHandler);
                    fail("Overwriting an existing node during import must throw a SAXException");
                } catch (SAXException e) {
                    // success
                }
            } else {
                try {
                    doImport(folder.getPath(), in, useWorkspace, useHandler);
                    fail("Overwriting an existing node during import must throw an ItemExistsException");
                } catch (ItemExistsException e) {
                    // success
                }
            }
        } finally {
            in.close();
        }
    }

    public void testOverwriteExceptionWorkspaceWithHandler() throws Exception {
        doTestOverwriteException(WORKSPACE, CONTENTHANDLER);
    }

    public void testOverwriteExceptionSessionWithHandler() throws Exception {
        doTestOverwriteException(SESSION, CONTENTHANDLER);
    }

    public void testOverwriteExceptionWorkspace() throws Exception {
        doTestOverwriteException(WORKSPACE, STREAM);
    }

    public void testOverwriteExceptionSession() throws Exception {
        doTestOverwriteException(SESSION, STREAM);
    }

    // ------------------< Node type constraint violation tests >--------------------------------
    /**
     * Create a node named ntBase with node type nt:base
     * and creates a tree in the repository which will be exported
     * and reimported below the node ntBase.
     *
     * @param useWorkspace
     * @param useHandler
     * @throws RepositoryException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void doTestNodeTypeConstraintViolation(boolean useWorkspace, boolean useHandler)
            throws Exception {

        treeComparator.createExampleTree();
        Node node = testRootNode.addNode("ntBase", ntBase);
        session.save();

        FileInputStream in = new FileInputStream(file);
        try {
            if (useHandler) {
                try {
                    doImport(node.getPath(), in, useWorkspace, useHandler);
                    fail("Node type constraint violation should throw a SAXException " +
                            "during xml import using a Contenthandler.");
                } catch (SAXException se) {
                    // ok
                }
            } else {
                try {
                    doImport(node.getPath(), in, useWorkspace, useHandler);
                    fail("Node type constraint violation should throw a  " +
                            " InvalidSerializedDataException during xml import " +
                            "using a Contenthandler.");
                } catch (InvalidSerializedDataException isde) {
                    // ok
                }
            }
        } finally {
            in.close();
        }
    }


    public void testNodeTypeConstraintViolationWorkspaceWithHandler() throws Exception {
        doTestNodeTypeConstraintViolation(WORKSPACE, CONTENTHANDLER);
    }

    public void testNodeTypeConstraintViolationSessionWithHandler() throws Exception {
        doTestNodeTypeConstraintViolation(SESSION, CONTENTHANDLER);
    }

    public void testNodeTypeConstraintViolationWorkspace() throws Exception {
        doTestNodeTypeConstraintViolation(WORKSPACE, STREAM);
    }

    public void testNodeTypeConstraintViolationSession() throws Exception {
        doTestNodeTypeConstraintViolation(SESSION, STREAM);
    }

// ------------< tests that nothing is imported if session is closed before saving the import >-----------------
    /**
     * Makes sure that importing into the session does not save anything if the
     * session is closed.
     */
    public void testSessionImportXml() throws RepositoryException, IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            exportRepository(SAVEBINARY, RECURSE);
            session.importXML(treeComparator.targetFolder, in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } finally {
            in.close();
        }

        // after logout/login, no nodes are in the session
        session.logout();
        superuser = null; //so tearDown won't fail

        session = helper.getReadWriteSession();
        treeComparator.setSession(session);
        treeComparator.compare(treeComparator.CHECK_EMPTY);
    }


    /**
     * Makes sure that importing into the session does not save anything if the
     * session is closed.
     */
    public void testSessionGetContentHandler() throws Exception {
        FileInputStream in = new FileInputStream(file);
        try {
            exportRepository(SAVEBINARY, RECURSE);
            doImportNoSave(treeComparator.targetFolder, in, CONTENTHANDLER);
        } finally {
            in.close();
        }

        // after logout/login, no nodes are in the session
        session.logout();
        superuser = null; //so tearDown won't fail

        session = helper.getReadWriteSession();
        treeComparator.setSession(session);
        treeComparator.compare(treeComparator.CHECK_EMPTY);
    }

//----------------< import test helper >--------------------------------------------------------
    /**
     * Helper method which imports the given FileInputStream using Workspace or Session
     * and via the methods importXML respective getImportContentHandler. Teh target node of the
     * import is specified with its absolut path.
     *
     * @param absPath
     * @param in
     * @param useWorkspace
     * @param useHandler
     * @throws RepositoryException
     * @throws IOException
     */
    public void doImport(String absPath, FileInputStream in, boolean useWorkspace, boolean useHandler)
            throws Exception {
        if (useHandler) {
            if (useWorkspace) {
                ContentHandler ih = workspace.getImportContentHandler(absPath, 0);
                createXMLReader(ih).parse(new InputSource(in));
            } else {
                ContentHandler ih = session.getImportContentHandler(absPath, 0);
                createXMLReader(ih).parse(new InputSource(in));
                session.save();
            }
        } else {
            if (useWorkspace) {
                workspace.importXML(absPath, in, 0);
            } else {
                session.importXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                session.save();
            }
        }
    }

    public void doImportNoSave(String absPath, FileInputStream in, boolean useHandler)
            throws Exception {
        if (useHandler) {
            ContentHandler ih = session.getImportContentHandler(absPath, 0);
            createXMLReader(ih).parse(new InputSource(in));
        } else {
            session.importXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }
    }
//------------< System view export import tests >-----------------------------------

    public void testExportSysView_stream_workspace_skipBinary_noRecurse() throws Exception {
        doTest(STREAM, WORKSPACE, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_stream_workspace_skipBinary_recurse() throws Exception {
        doTest(STREAM, WORKSPACE, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_stream_workspace_saveBinary_noRecurse() throws Exception {
        doTest(STREAM, WORKSPACE, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_stream_workspace_saveBinary_recurse() throws Exception {
        doTest(STREAM, WORKSPACE, SAVEBINARY, RECURSE);
    }

    public void testExportSysView_stream_session_skipBinary_noRecurse() throws Exception {
        doTest(STREAM, SESSION, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_stream_session_skipBinary_recurse() throws Exception {
        doTest(STREAM, SESSION, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_stream_session_saveBinary_noRecurse() throws Exception {
        doTest(STREAM, SESSION, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_stream_session_saveBinary_recurse() throws Exception {
        doTest(STREAM, SESSION, SAVEBINARY, RECURSE);
    }

    public void testExportSysView_handler_workspace_skipBinary_noRecurse() throws Exception {
        doTest(CONTENTHANDLER, WORKSPACE, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_handler_workspace_skipBinary_recurse() throws Exception {
        doTest(CONTENTHANDLER, WORKSPACE, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_handler_workspace_saveBinary_noRecurse() throws Exception {
        doTest(CONTENTHANDLER, WORKSPACE, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_handler_workspace_saveBinary_recurse() throws Exception {
        doTest(CONTENTHANDLER, WORKSPACE, SAVEBINARY, RECURSE);
    }

    public void testExportSysView_handler_session_skipBinary_noRecurse() throws Exception {
        doTest(CONTENTHANDLER, SESSION, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_handler_session_skipBinary_recurse() throws Exception {
        doTest(CONTENTHANDLER, SESSION, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_handler_session_saveBinary_noRecurse() throws Exception {
        doTest(CONTENTHANDLER, SESSION, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_handler_session_saveBinary_recurse() throws Exception {
        doTest(CONTENTHANDLER, SESSION, SAVEBINARY, RECURSE);
    }

    /**
     * Exports the tree at source node, imports it at the traget node, and
     * compares the source and target tree.
     *
     * @param handler    true = use content handler for import, false = use the
     *                   importXML method
     * @param workspace  true = import to the workspace, false = import to the
     *                   session (export is from session anyway)
     * @param skipBinary true = skip binary properties. The binary properties
     *                   are omitted (without any replacement)
     * @param noRecurse  true = export only top node, false = export entire
     *                   subtree
     */
    private void doTest(boolean handler, boolean workspace, boolean skipBinary, boolean noRecurse)
            throws Exception {
        exportRepository(skipBinary, noRecurse);
        importRepository(handler, workspace);
        treeComparator.showTree();
        treeComparator.compare(skipBinary, noRecurse);
    }

    /**
     * Exports the repository to a temporary file using the system view
     * serialization.
     *
     * @param skipBinary true = omit any binary properties (without any
     *                   replacement)
     * @param noRecurse  true = save only top node, false = save entire subtree
     * @throws IOException
     */
    private void exportRepository(boolean skipBinary, boolean noRecurse) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            session.refresh(false); //move the workspace into the session, then save it. The workspace is always valid, the session not necessarily.
            session.exportSystemView(treeComparator.getSourceRootPath(), out, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            fail("Could not export the repository: " + e);
        } finally {
            out.close();
        }
    }

    /**
     * Imports the repository
     *
     * @param useHandler True = use the import handler, false = use file input
     *                   stream
     * @param workspace  True = import into workspace, false = import into
     *                   session
     */
    public void importRepository(boolean useHandler, boolean workspace) throws Exception {
        FileInputStream in = new FileInputStream(file);
        try {
            if (useHandler) {
                if (workspace) {
                    ContentHandler ih = this.workspace.getImportContentHandler(treeComparator.targetFolder, 0);
                    createXMLReader(ih).parse(new InputSource(in));
                } else {
                    ContentHandler ih = session.getImportContentHandler(treeComparator.targetFolder,
                            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                    createXMLReader(ih).parse(new InputSource(in));
                    session.save();
                }
            } else {
                if (workspace) {
                    this.workspace.importXML(treeComparator.targetFolder, in, 0);
                } else {
                    session.importXML(treeComparator.targetFolder, in,
                            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                    session.save();
                }
            }
        } catch (SAXException e) {
            fail("Error while parsing the imported repository: " + e);
        } finally {
            in.close();
        }
    }

    /**
     * Creates an XMLReader for the given content handler.
     *
     * @param handler the content handler.
     * @return an XMLReader for the given content handler.
     * @throws SAXException if the reader cannot be created.
     */
    private XMLReader createXMLReader(ContentHandler handler) throws SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setFeature("http://xml.org/sax/features/namespaces", true);
        reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        reader.setContentHandler(handler);
        reader.setErrorHandler(new DefaultHandler());
        return reader;
    }
}
