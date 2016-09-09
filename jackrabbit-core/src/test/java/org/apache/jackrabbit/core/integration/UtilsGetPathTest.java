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
package org.apache.jackrabbit.core.integration;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @see <a href="https://issues.apache.org/jira/browse/JCR-3992">JCR-3992</a>
 *      and
 *      <a href="https://issues.apache.org/jira/browse/JCR-4015">JCR-4015</a>
 */
public class UtilsGetPathTest extends AbstractJCRTest {

    @Test
    public void testGetOrCreateByPath1() throws RepositoryException {
        String path = testRoot + "/foo";
        Node node = JcrUtils.getOrCreateByPath(path, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path, node.getPath());
        assertTrue(superuser.nodeExists(path));

        // existing top-level node, two new descendant nodes
        String path2 = testRoot + "/foo/a/b";
        Node node2 = JcrUtils.getOrCreateByPath(path2, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path2, node2.getPath());
        assertTrue(superuser.nodeExists(path2));
    }

    @Test
    public void testGetOrCreateByPathNoRoot() throws RepositoryException {
        String base = testRoot + "/foo";
        Node inter = JcrUtils.getOrCreateByPath(base, "nt:unstructured", superuser);
        assertEquals(base, inter.getPath());
        superuser.save();

        // test what happens if getRootNode() throws
        Session mockedSession = Mockito.spy(superuser);
        Mockito.when(mockedSession.getRootNode()).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.getNode("/")).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.getItem("/")).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.nodeExists("/")).thenReturn(false);

        Node result = JcrUtils.getOrCreateByPath(base + "/bar", false, null, null, mockedSession, false);
        mockedSession.save();
        assertEquals(base + "/bar", result.getPath());

        // already exists -> nop
        Node result2 = JcrUtils.getOrCreateByPath(base + "/bar", false, null, null, mockedSession, false);
        mockedSession.save();
        assertEquals(base + "/bar", result2.getPath());

        // create unique
        Node result3 = JcrUtils.getOrCreateByPath(base + "/bar", true, null, null, mockedSession, false);
        mockedSession.save();
        assertEquals(base + "/bar0", result3.getPath());

        // already exists with createUnique == false should pass even when parent isn't readable
        Mockito.when(mockedSession.getNode(base)).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.getItem(base)).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.nodeExists(base)).thenReturn(false);
        Node result4 = JcrUtils.getOrCreateByPath(base + "/bar", false, null, null, mockedSession, false);
        mockedSession.save();
        assertEquals(base + "/bar", result4.getPath());
    }
}
