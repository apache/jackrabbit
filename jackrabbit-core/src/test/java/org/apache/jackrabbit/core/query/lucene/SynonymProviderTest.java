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

import javax.jcr.RepositoryException;
import javax.jcr.Node;

import org.apache.jackrabbit.core.query.AbstractQueryTest;

/**
 * <code>SynonymProviderTest</code> contains test cases for the
 * <code>PropertiesSynonymProvider</code> class.
 * This test assumes that the following synonyms are defined:
 * <ul>
 * <li>quick &lt;-> fast</li>
 * <li>sluggish &lt;-> lazy</li>
 * <li>ASF &lt;-> Apache Software Foundation</li>
 * </ul>
 */
public class SynonymProviderTest extends AbstractQueryTest {

    public void testSynonyms() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty(propertyName1, "The quick brown fox jumps over the lazy dog.");
        testRootNode.save();
        executeXPathQuery(testPath + "//*[jcr:contains(., '~fast')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~Fast')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~quick')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~sluggish')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~sluGGish')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~lazy')]", new Node[]{n});
        // check term which is not in the synonym provider
        executeXPathQuery(testPath + "//*[jcr:contains(., '~brown')]", new Node[]{n});
    }

    public void testPhrase() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty(propertyName1, "Licensed to the Apache Software Foundation ...");
        testRootNode.save();
        executeXPathQuery(testPath + "//*[jcr:contains(., '~ASF')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., '~asf')]", new Node[]{n});
        executeXPathQuery(testPath + "//*[jcr:contains(., 'asf')]", new Node[]{});
    }

    public void disabled_testReload() throws RepositoryException, InterruptedException {
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1 * 1000);
            executeXPathQuery(testPath + "//*[jcr:contains(., '~asf')]", new Node[]{});
        }
    }
}
