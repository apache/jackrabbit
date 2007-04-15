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
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * The <code>DynamicRepositoryClassLoaderTest</code> class
 *
 * @author Felix Meschberger
 */
public class DynamicRepositoryClassLoaderTest extends ClassLoaderTestBase {

    private static final String CLASSES_FOLDER = "/node1/classes";
    private static final String JAR_FILE = "/node1/mock.jar";
    private static final String CLASS_FILE = "org/apache/jackrabbit/classloader/Util.class";
    private static final String JAR_FILE_ENTRY = "mock/aDir/anotherFile.txt";
    private static final String NON_EXISTING_JAR_FILE_ENTRY = "mock/aDir/missingFile.txt";

    private static final String[] handles = { CLASSES_FOLDER, JAR_FILE };

    private DynamicRepositoryClassLoader loader;

    protected void setUp() throws Exception {
        super.setUp();

        loadRepository(session, null);

        loader = new DynamicRepositoryClassLoader(session, handles, null);
    }

    protected void tearDown() throws Exception {
        if (loader != null) {
            loader.destroy();
            loader = null;
        }

        super.tearDown();
    }

    public void testGetURLs() {
        URL[] urls = loader.getURLs();

        /*
         * Expected URLs
         *   urls[0] = jcr:/_/default/node1/classes/
         *   urls[1] = jar:jcr:/_/default/node1/mock.jar
         */

        assertNotNull("Class loader URLs", urls);
        assertEquals("Number of class path entries", handles.length, urls.length);
        assertEquals("URL " + CLASSES_FOLDER, "jcr:/_/" + WORKSPACE + CLASSES_FOLDER + "/", urls[0].toString());
        assertEquals("URL " + JAR_FILE, "jar:jcr:/_/" + WORKSPACE + JAR_FILE, urls[1].toString());
    }

    public void testClassFile() {
        URL resource = loader.getResource(CLASS_FILE);
        assertNotNull("Resource " + CLASS_FILE, resource);
    }

    public void testJarEntry() {
        URL resource = loader.getResource(JAR_FILE_ENTRY);
        assertNotNull("Resource " + JAR_FILE_ENTRY, resource);

        resource = loader.getResource(NON_EXISTING_JAR_FILE_ENTRY);
        assertNull("Resource " + NON_EXISTING_JAR_FILE_ENTRY + " not expected", resource);
    }

    public void testResources() throws IOException {
        Enumeration res = loader.getResources(CLASS_FILE);
        assertTrue("At least one resource " + CLASS_FILE, res.hasMoreElements());

        URL url = (URL) res.nextElement();
        assertNotNull(url);

        assertFalse("Only expecint one resource " + CLASS_FILE, res.hasMoreElements());
    }
}
