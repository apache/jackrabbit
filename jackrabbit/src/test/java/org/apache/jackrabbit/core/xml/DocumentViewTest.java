/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.core.TestRepository;

import junit.framework.TestCase;

/**
 * Jackrabbit-specific test cases for the document view XML format.
 *
 * @see org.apache.jackrabbit.test.api.ExportDocViewTest
 * @see org.apache.jackrabbit.test.api.DocumentViewImportTest
 */
public class DocumentViewTest extends TestCase {

    /**
     * Test case for
     * <a href="http://issues.apache.org/jira/browse/JCR-369">JCR-369</a>:
     * IllegalNameException when importing document view with two mixins.
     *
     * @throws Exception if an unexpected error occurs
     */
    public void testTwoMixins() throws Exception {
        Session session = TestRepository.getInstance().login();
        try {
            String xml = "<two-mixins-test"
                + " jcr:mixinTypes=\"mix:referenceable mix:lockable\""
                + " xmlns:jcr=\"http://www.jcp.org/jcr/1.0\""
                + " xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\"/>";
            InputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            session.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } catch (ValueFormatException e) {
            fail("JCR-369: IllegalNameException when importing document view"
                    + " with two mixins");
        } finally {
            session.logout();
        }
    }

}
