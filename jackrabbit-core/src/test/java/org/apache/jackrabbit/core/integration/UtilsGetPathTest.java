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
        String path ="/foo";
        Node node = JcrUtils.getOrCreateByPath(path, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path, node.getPath());
        assertTrue(superuser.nodeExists(path));

        // existing top-level node, two new descendant nodes
        String path2 ="/foo/a/b";
        Node node2 = JcrUtils.getOrCreateByPath(path2, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path2, node2.getPath());
        assertTrue(superuser.nodeExists(path2));
    }

    @Test
    public void testGetOrCreateByPathNoRoot() throws RepositoryException {
        Node inter = JcrUtils.getOrCreateByPath("/foo", "nt:unstructured", superuser);
        assertEquals("/foo", inter.getPath());
        superuser.save();

        // test what happens if getRootNode() throws
        final Session mockedSession = Mockito.spy(superuser);
        Mockito.when(mockedSession.getRootNode()).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.getNode("/")).thenThrow(new AccessDeniedException("access denied"));
        Mockito.when(mockedSession.getItem("/")).thenThrow(new AccessDeniedException("access denied"));

        Node result = JcrUtils.getOrCreateByPath("/foo/bar", false, null, null, mockedSession, false);
        superuser.save();
        assertEquals("/foo/bar", result.getPath());
    }
}
