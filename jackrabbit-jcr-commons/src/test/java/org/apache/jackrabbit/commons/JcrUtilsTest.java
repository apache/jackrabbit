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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;

public class JcrUtilsTest extends MockCase {

    public void testGetRepository() throws Exception {
        Object repository = record(AbstractRepository.class);

        Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(
                "java.naming.factory.initial",
                "org.osjava.sj.memory.MemoryContextFactory");
        environment.put("org.osjava.sj.jndi.shared", "true");
        InitialContext context = new InitialContext(environment);
        context.bind("repository", repository);

        // Test lookup with a traditional map of parameters
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(
                "org.apache.jackrabbit.repository.jndi.name", "repository");
        parameters.put(
                "java.naming.factory.initial",
                "org.osjava.sj.memory.MemoryContextFactory");
        parameters.put("org.osjava.sj.jndi.shared", "true");
        assertTrue(repository == JcrUtils.getRepository(parameters));

        // Test lookup with URI query parameters
        assertTrue(repository == JcrUtils.getRepository(
                "jndi://x"
                + "?org.apache.jackrabbit.repository.jndi.name=repository"
                + "&org.osjava.sj.jndi.shared=true"
                + "&java.naming.factory.initial"
                + "=org.osjava.sj.memory.MemoryContextFactory"));

        // Test lookup with the custom JNDI URI format (JCR-2771)
        assertTrue(repository == JcrUtils.getRepository(
                "jndi://org.osjava.sj.memory.MemoryContextFactory/repository"
                + "?org.osjava.sj.jndi.shared=true"));

        try {
            JcrUtils.getRepository(
                    "jndi://org.osjava.sj.memory.MemoryContextFactory/missing");
            fail("Repository lookup failure should throw an exception");
        } catch (RepositoryException expected) {
        }
    }

    public void testGetPropertyType() {
        assertEquals(PropertyType.BINARY, JcrUtils.getPropertyType(
                PropertyType.TYPENAME_BINARY));
        assertEquals(PropertyType.BOOLEAN, JcrUtils.getPropertyType(
                PropertyType.TYPENAME_BOOLEAN.toLowerCase()));
        assertEquals(PropertyType.DATE, JcrUtils.getPropertyType(
                PropertyType.TYPENAME_DATE.toUpperCase()));
    }

    public void testIn() {
        Iterable<String> iterable = JcrUtils.in(Arrays.asList("A").iterator());
        Iterator<String> iterator = iterable.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse(iterator.hasNext());

        try {
            iterable.iterator();
            fail("Second execution of Iterable.iterator() should throw an exception");
        } catch (IllegalStateException expected) {
        }
    }
}
