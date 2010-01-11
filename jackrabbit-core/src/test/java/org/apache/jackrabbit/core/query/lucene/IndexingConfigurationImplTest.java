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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.AbstractQueryTest;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <code>IndexingConfigurationImplTest</code>...
 */
public class IndexingConfigurationImplTest extends AbstractQueryTest {

    private static final Name FOO = NameFactoryImpl.getInstance().create("", "foo");

    private NodeState nState;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Node n = testRootNode.addNode(nodeName1, ntUnstructured);
        n.addMixin(mixReferenceable);
        superuser.save();
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
