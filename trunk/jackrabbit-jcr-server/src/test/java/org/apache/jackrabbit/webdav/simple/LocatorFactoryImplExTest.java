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
package org.apache.jackrabbit.webdav.simple;

import junit.framework.TestCase;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;

/** <code>LocatorFactoryImplExTest</code>... */
public class LocatorFactoryImplExTest extends TestCase {

    private DavLocatorFactory factory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // for simplicity (not yet used) ignore the path prefix.
        factory = new LocatorFactoryImplEx(null);
    }

    /**
     * Test for issue https://issues.apache.org/jira/browse/JCR-1679: An top
     * level resource (node directly below the root) whose name equals the
     * workspace name results in wrong collection behaviour (garbeled locator
     * of child resources).
     */
    public void testCollectionNameEqualsWorkspaceName() {
        String prefix = "http://localhost:8080/jackrabbit/repository";
        String workspacePath = "/default";
        String nodePath = "/default/another";

        DavResourceLocator locator = factory.createResourceLocator(prefix, workspacePath, nodePath, false);
        assertTrue(locator.getHref(true).indexOf("/default/default") > 0);

        DavResourceLocator locator2 = factory.createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), locator.getResourcePath());
        assertEquals(locator, locator2);
        assertEquals(nodePath, locator2.getRepositoryPath());
    }
}
