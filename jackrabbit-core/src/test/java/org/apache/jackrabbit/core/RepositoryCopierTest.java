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

import java.io.File;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryCopier;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class RepositoryCopierTest extends TestCase {

    private static final Credentials CREDENTIALS =
        new SimpleCredentials("admin", "admin".toCharArray());

    private final File BASE = new File("target", "RepositoryCopierTest");

    private final File SOURCE = new File(BASE, "source");

    private final File TARGET = new File(BASE, "target");

    protected void setUp() {
        BASE.mkdirs();
    }

    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(BASE);
    }

    public void testRepositoryCopy() throws Exception {
        createSourceRepository();
        new RepositoryCopier(SOURCE, TARGET).copy();
        verifyTargetRepository();
    }

    private void createSourceRepository() throws Exception {
        RepositoryImpl repository = RepositoryImpl.create(
                RepositoryConfig.install(SOURCE));
        try {
            Session session = repository.login(CREDENTIALS);
            try {
                Node test = session.getRootNode().addNode("test");
                test.setProperty("foo", "bar");
                session.save();
            } finally {
                session.logout();
            }
        } finally {
            repository.shutdown();
        }
    }

    private void verifyTargetRepository() throws Exception {
        RepositoryImpl repository = RepositoryImpl.create(
                RepositoryConfig.create(TARGET));
        try {
            Session session = repository.login(CREDENTIALS);
            try {
                assertTrue(session.nodeExists("/test"));
                assertTrue(session.propertyExists("/test/foo"));

                Property foo = session.getProperty("/test/foo");
                assertEquals(PropertyType.STRING, foo.getType());
                assertEquals("bar", foo.getString());
            } finally {
                session.logout();
            }
        } finally {
            repository.shutdown();
        }
    }

}
