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
package org.apache.jackrabbit.extension;

import java.util.Iterator;

import org.apache.commons.configuration.Configuration;

public class ExtensionFindTest extends ExtensionFrameworkTestBase {

    protected ExtensionManager em;

    protected void setUp() throws Exception {
        super.setUp();
        ExtensionManager.checkNodeType(session);
        fillTestData(session);
        em = new ExtensionManager(session, getClass().getClassLoader());
    }

    protected void tearDown() throws Exception {
        em = null;
        removeTestData(session);
        super.tearDown();
    }

    public void testFindings() throws ExtensionException {

        Iterator ei = em.getExtensions(ID1, ROOT_NODE);
        while (ei.hasNext()) {
            ExtensionDescriptor ed = (ExtensionDescriptor) ei.next();
            System.out.println("Extension " + ed.getId() + "/" + ed.getName() + " (" + ed.getNodePath() +")");

            if (ed.getClassName() != null) {
                Object ext = ed.getExtension();
                System.out.println("   Class       : " + ed.getClassName());
                System.out.println("   ClassLoader : " + ext.getClass().getClassLoader());
                System.out.println("   Extension   : " + ext);
            }

            Configuration config = ed.getConfiguration();
            System.out.println("   Configuration Class : " + config.getClass().getName());
            System.out.println("   Configuration       : " + config);
            for (Iterator ki=config.getKeys(); ki.hasNext(); ) {
                String key = (String) ki.next();
                Object prop = config.getProperty(key);
                System.out.println("     " + key + " ==> " + prop);
            }
        }
    }

    public void testFindNonExisting() {
        try {
            em.getExtension(ID1, "google", ROOT_NODE);
        } catch (ExtensionException ee) {
            assertTrue("Wrong exception " + ee + " thrown",
                ee.getMessage() != null &&
                    ee.getMessage().indexOf("not found") > 0);
        }
    }

    public void testFindExisting() throws ExtensionException {
        em.getExtension(ID1, "delivery.core", ROOT_NODE);
    }

    public void testFindMultipleExisting() {
        try {
            em.getExtension(ID1, "delivery.gfx", ROOT_NODE);
        } catch (ExtensionException ee) {
            assertTrue("Wrong exception " + ee + " thrown",
                ee.getMessage() != null &&
                    ee.getMessage().indexOf("ore than one extension") > 0);
        }
    }
}
