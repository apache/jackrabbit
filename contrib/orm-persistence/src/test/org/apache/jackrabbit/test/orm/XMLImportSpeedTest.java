/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.orm;

import java.io.FileInputStream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import javax.jcr.ImportUUIDBehavior;

/**
 * Test suite for XML import speed benchmarking.
 */
public class XMLImportSpeedTest extends AbstractJCRTest {

    private static final int BLOB_NODE_COUNT = 2;
    private static final int XML_IMPORT_NODE_COUNT = 1;

    private final String XML_IMPORT_FILENAME = "applications/test/import.xml";

    private int totalNodeCount = 0;
    private int totalElementCount = 0;
    private int totalAttributeCount = 0;

    public void treeWalk(Document document) {
        treeWalk(document.getRootElement());
    }

    public void treeWalk(Element element) {
        totalElementCount++;
        totalAttributeCount += element.attributeCount();
        for (int i = 0, size = element.nodeCount(); i < size; i++) {
            totalNodeCount++;
            org.dom4j.Node node = element.node(i);
            if (node instanceof Element) {
                treeWalk( (Element) node);
            } else {
                // do something....
            }
        }
    }

    public void testXMLImportSpeed() throws Exception {
        Node rn = superuser.getRootNode();

        if (!rn.hasNode("importxml0")) {
            log.println("importing xml");

            SAXReader reader = new SAXReader();
            Document document = reader.read(new FileInputStream(
                XML_IMPORT_FILENAME));
            treeWalk(document);
            log.println("XML file " + XML_IMPORT_FILENAME + " has " +
                        totalElementCount + " elements, " + totalNodeCount +
                        " nodes and " + totalAttributeCount +
                        " attributes");

            log.println("Now performing XML import " +
                        XML_IMPORT_NODE_COUNT +
                        " time(s)...");
            long xmlImportTestStart = System.currentTimeMillis();
            for (int i = 0; i < XML_IMPORT_NODE_COUNT; i++) {
                Node nl = rn.addNode("importxml" + i, "nt:unstructured");
                superuser.importXML("/importxml" + i,
                                    new FileInputStream(
                                        XML_IMPORT_FILENAME), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                log.println("Saving...");
                superuser.save();
            }
            long xmlImportTestTime = System.currentTimeMillis() -
                xmlImportTestStart;
            log.println("Imported XML " + XML_IMPORT_NODE_COUNT +
                        " time(s) in " +
                        xmlImportTestTime + "ms average=" +
                        xmlImportTestTime / XML_IMPORT_NODE_COUNT +
                        "ms/node");

            log.println("Removing imported node(s)...");
            for (int i = 0; i < XML_IMPORT_NODE_COUNT; i++) {
                Node curXMLImportNode = rn.getNode("importxml" + i);
                curXMLImportNode.remove();
            }
            superuser.save();
        } else {
            log.println(
                "XML import has already been run previously, not reimporting on same nodes");
        }

        // dump(rn);

    }

    public void dump(Node n) throws RepositoryException {
        log.println(n.getPath());
        PropertyIterator pit = n.getProperties();
        while (pit.hasNext()) {
            Property p = pit.nextProperty();
            if (!p.getDefinition().isMultiple()) {
                Value curValue = p.getValue();
                if (curValue.getType() == PropertyType.BINARY) {
                    log.println(p.getPath() + "=BINARY[" + p.getLength() + "]");
                } else {
                    log.println(p.getPath() + "=" + p.getString());
                }
            } else {
                log.println("Multi-value for " + p.getPath());
            }
        }
        NodeIterator nit = n.getNodes();
        while (nit.hasNext()) {
            Node cn = nit.nextNode();
            dump(cn);
        }
    }

}
