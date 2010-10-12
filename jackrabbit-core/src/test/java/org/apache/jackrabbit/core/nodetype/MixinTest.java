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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 * <code>MixinTest</code>...
 */
public class MixinTest extends AbstractJCRTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(MixinTest.class);

    /** Name of the cnd nodetype file for import and namespace registration. */
    private static final String TEST_NODETYPES = "org/apache/jackrabbit/core/nodetype/xml/test_mixin_nodetypes.cnd";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Reader cnd = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(TEST_NODETYPES));
        CndImporter.registerNodeTypes(cnd, superuser);
        cnd.close();
    }

    /**
     * Test for bug JCR-2778
     *
     * @throws Exception
     */
    public void testMixinRemovedWithProtectedChildNode() throws Exception {
        testRootNode.addMixin("test:mixinNode_protectedchild");
        superuser.save();

        // remove the mixin type again
        testRootNode.removeMixin("test:mixinNode_protectedchild");

        assertFalse(testRootNode.isNodeType("test:mixinNode_protectedchild"));
        superuser.save();
        assertFalse(testRootNode.isNodeType("test:mixinNode_protectedchild"));
    }
}