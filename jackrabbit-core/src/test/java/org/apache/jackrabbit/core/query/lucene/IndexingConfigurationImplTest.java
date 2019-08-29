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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <code>IndexingConfigurationImplTest</code>...
 */
public class IndexingConfigurationImplTest extends AbstractIndexingTest {

    private static final Name FOO = NameFactoryImpl.getInstance().create("", "foo");
    private static final Name OTHER = NameFactoryImpl.getInstance().create("", "other");

    private NodeState nState;
    private Node n;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        n = testRootNode.addNode(nodeName1, ntUnstructured);
        n.addMixin(mixReferenceable);
        n.addMixin(mixTitle);
        session.save();
        nState = (NodeState) getSearchIndex().getContext().getItemStateManager().getItemState(
                new NodeId(n.getIdentifier()));
    }

    public void testMatchAllNoPrefix() throws Exception {
        IndexingConfiguration config = createConfig("config1");
        assertFalse(config.isIndexed(nState, NameConstants.JCR_DATA));
        assertTrue(config.isIndexed(nState, FOO));
    }

    public void testRegexpInPrefix() throws Exception {
        IndexingConfiguration config = createConfig("config2");
        assertTrue(config.isIndexed(nState, NameConstants.JCR_DATA));
        assertTrue(config.isIndexed(nState, FOO));
    }

    public void testMatchAllJCRPrefix() throws Exception {
        IndexingConfiguration config = createConfig("config3");
        assertTrue(config.isIndexed(nState, NameConstants.JCR_DATA));
        assertFalse(config.isIndexed(nState, FOO));
    }

    public void testAddNodeTypeToRegistry() throws Exception {
        IndexingConfiguration config = createConfig("config4");
        // add node type
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        String baseName = "indexingTextNodeType";
        int i = 0;
        String nt;
        do {
            nt = baseName + "_" + i++;
        } while (ntMgr.hasNodeType(nt));
        // register node type
        NodeTypeTemplate ntTemplate = ntMgr.createNodeTypeTemplate();
        ntTemplate.setName(nt);
        ntTemplate.setDeclaredSuperTypeNames(new String[]{ntUnstructured});
        ntMgr.registerNodeType(ntTemplate, false);
        // create node
        Node n = testRootNode.addNode(nodeName2, nt);
        session.save();
        // get state
        NodeState state = (NodeState) getSearchIndex().getContext().getItemStateManager().getItemState(
                new NodeId(n.getIdentifier()));
        assertTrue(config.isIndexed(state, FOO));
        assertFalse(config.isIncludedInNodeScopeIndex(state, FOO));
    }

    public void testIndexRuleMixin() throws Exception{
        IndexingConfiguration config = createConfig("config5");
        assertTrue(config.isIndexed(nState, NameConstants.JCR_TITLE));
        assertFalse(config.isIndexed(nState, NameConstants.JCR_DESCRIPTION));
        assertTrue(config.isIndexed(nState, NameConstants.JCR_UUID)); // from mixReferenceable ... should be indexed
    }

    public void testMatchCondition() throws Exception{
        IndexingConfiguration config = createConfig("config6");
        Node n = testRootNode.addNode(nodeName1, ntUnstructured);
        n.addMixin(mixReferenceable);
        n.setProperty(FOO.getLocalName(), "high");
        session.save();
        nState = (NodeState) getSearchIndex().getContext().getItemStateManager().getItemState(
                new NodeId(n.getIdentifier()));        
        assertTrue(config.isIndexed(nState, FOO));
        assertFalse(config.isIndexed(nState, OTHER));
        
        n = testRootNode.addNode(nodeName2, ntUnstructured);
        n.addMixin(mixReferenceable);
        session.save();
        nState = (NodeState) getSearchIndex().getContext().getItemStateManager().getItemState(
                new NodeId(n.getIdentifier()));        
        assertTrue(config.isIndexed(nState, OTHER));
        assertFalse(config.isIndexed(nState, FOO));
    }

    //----------------------------< internal >----------------------------------
    protected IndexingConfiguration createConfig(String name) throws Exception {
        IndexingConfiguration config = new IndexingConfigurationImpl();
        config.init(loadConfig(name), getSearchIndex().getContext(),
                getSearchIndex().getNamespaceMappings());
        return config;
    }

    protected Element loadConfig(String name)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new IndexingConfigurationEntityResolver());
        InputStream in = getClass().getResourceAsStream("indexing_" + name + ".xml");
        return builder.parse(in).getDocumentElement();
    }
}
