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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * Test case for JCR-1475.
 */
public class RemoveVersionLabelTest extends AbstractJCRTest {

    public void testRemoveVersionLabel() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        Version v10 = n.checkin();
        n.checkout();
        n.checkin();
        VersionHistory vh = n.getVersionHistory();
        vh.addVersionLabel(v10.getName(), "test", true);
        // the next call must not fail
        vh.removeVersion(v10.getName());
        // now the label must be gone
        String[] labels = vh.getVersionLabels();
        assertEquals("Label of a removed version must be removed as well", 0, labels.length);
    }
}
