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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.test.JUnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>AbstractPolicyTemplateTest</code>...
 */
public abstract class AbstractPolicyTemplateTest extends JUnitTest {

    private static Logger log = LoggerFactory.getLogger(AbstractPolicyTemplateTest.class);

    protected Principal testPrincipal;

    protected void setUp() throws Exception {
        super.setUp();
        testPrincipal = new Principal() {
            public String getName() {
                return "TestPrincipal";
            }
        };
    }

    protected abstract String getTestPath();
    
    protected abstract PolicyTemplate createEmptyTemplate(String path);

    public void testEmptyTemplate() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());

        assertNotNull(pt.getEntries());
        assertTrue(pt.getEntries().length == 0);
        assertTrue(pt.isEmpty());
        assertNotNull(pt.getName());
    }


    public void testGetPath() {
        PolicyTemplate pt = (PolicyTemplate) createEmptyTemplate(getTestPath());
        assertEquals(getTestPath(), pt.getPath());
    }

    // TODO: add more tests
}