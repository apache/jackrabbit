/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.test;

import javax.jcr.Session;
import javax.jcr.Node;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public abstract class AbstractTest extends JUnitTest {

    protected static final String TEST_ROOT = "testroot";

    protected static final String JCR_PRIMARY_TYPE = "jcr:primaryType";

    protected static final String NT_UNSTRUCTURED = "nt:unstructured";

    protected static final String MIX_REFERENCABLE = "mix:referencable";

    protected static final String NT_BASE = "nt:base";

    /** The superuser session */
    protected Session superuser;

    /** The root <code>Node</code> for testing */
    protected Node testRoot;

    protected void setUp() throws Exception {
        superuser = helper.getSuperuserSession();
        Node root = superuser.getRootNode();
        if (root.hasNode(TEST_ROOT)) {
            // remove test root
            root.remove(TEST_ROOT);
            root.save();
        }
        testRoot = root.addNode(TEST_ROOT, NT_UNSTRUCTURED);
        root.save();
    }

    protected void tearDown() throws Exception {
        if (superuser != null) {
            // do a 'rollback'
            superuser.refresh(false);
            Node root = superuser.getRootNode();
            if (root.hasNode(TEST_ROOT)) {
                root.remove(TEST_ROOT);
                root.save();
            }
            superuser.logout();
        }
    }

}
