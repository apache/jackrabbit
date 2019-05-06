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

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.test.NotExecutableException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * <code>ConcurrentVersioningTest</code> contains test cases that run version
 * operations with concurrent threads.
 */
public class ConcurrentImportTest extends AbstractConcurrencyTest {

    private static final Attributes EMPTY_ATTRS = new AttributesImpl();

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 4;

    /**
     * The total number of operations to execute. E.g. number of checkins
     * performed by the threads.
     */
    private static final int NUM_NODES = 10;
    
    public void testConcurrentImport() throws RepositoryException {
        try {
            concurrentImport(new String[]{JcrConstants.MIX_REFERENCEABLE}, false);
        }
        finally {
            checkConsistency();
        }
    }

    public void testConcurrentImportSynced() throws RepositoryException {
        concurrentImport(new String[]{JcrConstants.MIX_REFERENCEABLE}, true);
    }

    public void testConcurrentImportVersionable() throws RepositoryException {
        concurrentImport(new String[]{JcrConstants.MIX_VERSIONABLE}, false);
    }

    public void testConcurrentImportVersionableSynced() throws RepositoryException {
        concurrentImport(new String[]{JcrConstants.MIX_VERSIONABLE}, true);
    }

    private void concurrentImport(final String[] mixins, final boolean sync) throws RepositoryException {
        final String[] uuids = new String[NUM_NODES];
        for (int i=0; i<uuids.length; i++) {
            uuids[i] = UUID.randomUUID().toString();
        }
        log.println("concurrentImport: c=" + CONCURRENCY + ", n=" + NUM_NODES);
        log.flush();

        final Lock lock = new ReentrantLock();
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                try {
                    // add versionable nodes
                    for (String uuid : uuids) {
                        if (sync) {
                            lock.lock();
                        }
                        try {
                            session.refresh(false);
                            try {
                                addNode(test, uuid,
                                        JcrConstants.NT_UNSTRUCTURED, uuid,
                                        mixins);
                                session.save();
                                log.println("Added " + test.getPath() + "/"
                                        + uuid);
                                log.flush();
                            } catch (InvalidItemStateException e) {
                                log.println("Ignoring expected error: " + e.toString());
                                log.flush();
                                session.refresh(false);
                            }
                        } finally {
                            if (sync) {
                                lock.unlock();
                            }
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                } catch (RepositoryException e) {
                    log.println("Error: " + e);
                    log.flush();
                    throw e;
                }
            }
        }, CONCURRENCY, "/" + testPath);
    }

    /**
     * Adds a new node with the given type and uuid. If a node with the same
     * uuid already exists, an exception is thrown.
     *
     * @param parent parent node
     * @param name node name
     * @param type primary node type
     * @param uuid uuid or <code>null</code>
     * @param mixins mixins
     * @return the new node.
     * @throws RepositoryException if an error occurs
     */
    public static Node addNode(Node parent, String name, String type, String uuid, String[] mixins)
            throws RepositoryException {
        try {
            final Session session = parent.getSession();
            final ContentHandler handler = session.getImportContentHandler(
                    parent.getPath(),
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

            // first define the current namespaces
            String[] prefixes = session.getNamespacePrefixes();
            handler.startDocument();
            for (String prefix : prefixes) {
                handler.startPrefixMapping(prefix, session.getNamespaceURI(prefix));
            }
            AttributesImpl attrs = new AttributesImpl();

            attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", name);
            handler.startElement(Name.NS_SV_URI, "node", "sv:node", attrs);

            // add the jcr:primaryTye
            attrs = new AttributesImpl();
            attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", JcrConstants.JCR_PRIMARYTYPE);
            attrs.addAttribute(Name.NS_SV_URI, "type", "sv:type", "CDATA", "Name");
            handler.startElement(Name.NS_SV_URI, "property", "sv:property", attrs);
            handler.startElement(Name.NS_SV_URI, "value", "sv:value", EMPTY_ATTRS);
            handler.characters(type.toCharArray(), 0, type.length());
            handler.endElement(Name.NS_SV_URI, "value", "sv:value");
            handler.endElement(Name.NS_SV_URI, "property", "sv:property");

            if (mixins.length > 0) {
                // add the jcr:mixinTypes
                attrs = new AttributesImpl();
                attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", JcrConstants.JCR_MIXINTYPES);
                attrs.addAttribute(Name.NS_SV_URI, "type", "sv:type", "CDATA", "Name");
                handler.startElement(Name.NS_SV_URI, "property", "sv:property", attrs);
                for (String mix: mixins) {
                    handler.startElement(Name.NS_SV_URI, "value", "sv:value", EMPTY_ATTRS);
                    handler.characters(mix.toCharArray(), 0, mix.length());
                    handler.endElement(Name.NS_SV_URI, "value", "sv:value");
                }
                handler.endElement(Name.NS_SV_URI, "property", "sv:property");
            }

            // add the jcr:uuid
            if (uuid != null) {
                attrs = new AttributesImpl();
                attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", JcrConstants.JCR_UUID);
                attrs.addAttribute(Name.NS_SV_URI, "type", "sv:type", "CDATA", "String");
                handler.startElement(Name.NS_SV_URI, "property", "sv:property", attrs);
                handler.startElement(Name.NS_SV_URI, "value", "sv:value", EMPTY_ATTRS);
                handler.characters(uuid.toCharArray(), 0, uuid.length());
                handler.endElement(Name.NS_SV_URI, "value", "sv:value");
                handler.endElement(Name.NS_SV_URI, "property", "sv:property");
            }


            handler.endElement(Name.NS_SV_URI, "node", "sv:node");
            handler.endDocument();

            return parent.getNode(name);

        } catch (SAXException e) {
            Exception root = e.getException();
            if (root instanceof RepositoryException) {
                throw (RepositoryException) root;
            } else if (root instanceof RuntimeException) {
                throw (RuntimeException) root;
            } else {
                throw new RepositoryException("Error while creating node", root);
            }
        }

    }

    private void checkConsistency() throws RepositoryException {
        try {
            ConsistencyReport rep = TestHelper.checkConsistency(testRootNode.getSession(), false, null);
            assertEquals("Found broken nodes in repository: " + rep, 0, rep.getItems().size());
        } catch (NotExecutableException ex) {
            // ignore
        }
    }
}
