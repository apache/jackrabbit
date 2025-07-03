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
package org.apache.jackrabbit.commons;

import junit.framework.TestCase;
import org.mockito.Mockito;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class NamespaceHelperTest extends TestCase {

    private Session session;

    protected void setUp() throws Exception {
        super.setUp();
        session = Mockito.mock(Session.class);
    }

    public void testGetNamespaces() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        when(session.getNamespacePrefixes()).thenReturn(new String[0]);
        Map<String, String> map = nsHelper.getNamespaces();
        assertEquals(0, map.size());

        when(session.getNamespacePrefixes()).thenReturn(new String[]{"foo"});
        map = nsHelper.getNamespaces();
        assertEquals(1, map.size());
        assertNull(map.get("foo"));

        when(session.getNamespacePrefixes()).thenReturn(new String[]{"foo", "bar"});
        when(session.getNamespaceURI("bar")).thenReturn("urn:test");
        map = nsHelper.getNamespaces();
        assertEquals(2, map.size());
        assertNull(map.get("foo"));
        assertEquals("urn:test", map.get("bar"));
    }

    public void testGetPrefix() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        String uri = "urn:uuid" + UUID.randomUUID();

        String prefix = nsHelper.getPrefix(uri);
        assertNull(prefix);

        prefix = nsHelper.getPrefix(uri);
        assertNull(prefix);

        when(session.getNamespacePrefix(uri)).thenReturn("foo");
        prefix = nsHelper.getPrefix(uri);
        assertEquals("foo", prefix);
    }
}
