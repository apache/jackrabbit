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
package org.apache.jackrabbit.spi.commons.privilege;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * <code>PrivilegeReadTest</code>...
 */
public class PrivilegeHandlerTest extends TestCase {

    private static final NameFactory NF = NameFactoryImpl.getInstance();
    /*
    <privilege name="foo:testRead"/>
    <privilege name="foo:testWrite"/>
    <privilege abstract="true" name="foo:testAbstract"/>
    <privilege abstract="false" name="foo:testNonAbstract"/>
    <privilege name="foo:testAll">
        <contains name="foo:testRead"/>
        <contains name="foo:testWrite"/>
    </privilege>

     */
    private static final String TEST_PREFIX = "foo";
    private static final String TEST_URI = "http://www.foo.com/1.0";

    private static PrivilegeDefinition DEF_READ = new PrivilegeDefinition(NF.create(TEST_URI,"testRead"), false, null);
    private static PrivilegeDefinition DEF_WRITE = new PrivilegeDefinition(NF.create(TEST_URI,"testWrite"), false, null);
    private static PrivilegeDefinition DEF_ABSTRACT = new PrivilegeDefinition(NF.create(TEST_URI,"testAbstract"), true, null);
    private static PrivilegeDefinition DEF_NON_ABSTRACT = new PrivilegeDefinition(NF.create(TEST_URI,"testNonAbstract"), false, null);
    private static Set<Name> aggr = new LinkedHashSet<Name>();
    static {
        aggr.add(DEF_READ.getName());
        aggr.add(DEF_WRITE.getName());
    }
    private static PrivilegeDefinition DEF_ALL = new PrivilegeDefinition(NF.create(TEST_URI,"testAll"), false, aggr);
    private static PrivilegeDefinition[] DEF_EXPECTED = new PrivilegeDefinition[]{
            DEF_READ,
            DEF_WRITE,
            DEF_ABSTRACT,
            DEF_NON_ABSTRACT,
            DEF_ALL
    };

    public void testRead() throws Exception {
        InputStream in = getClass().getResourceAsStream("readtest.xml");

        PrivilegeDefinitionReader reader = new PrivilegeDefinitionReader(in, "text/xml");

        Map<Name, PrivilegeDefinition> defs = new HashMap<Name, PrivilegeDefinition>();
        for (PrivilegeDefinition def: reader.getPrivilegeDefinitions()) {
            defs.put(def.getName(), def);
        }
        for (PrivilegeDefinition def: DEF_EXPECTED) {
            PrivilegeDefinition e = defs.remove(def.getName());
            assertNotNull("Definition " + def.getName() + " missing");
            assertEquals("Definition mismatch.", def,  e);
        }
        assertTrue("Not all definitions present", defs.isEmpty());

        // check for namespace
        String fooUri = reader.getNamespaces().get(TEST_PREFIX);
        assertEquals("Namespace included", TEST_URI, fooUri);
    }

    public void testWrite() throws Exception {

        PrivilegeDefinitionWriter writer = new PrivilegeDefinitionWriter("text/xml");
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put(TEST_PREFIX, TEST_URI);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.writeDefinitions(out, DEF_EXPECTED, namespaces);

        String result = out.toString("utf-8").trim();

        byte[] buffer = new byte[0x10000];
        int read = getClass().getResourceAsStream("writetest.xml").read(buffer);
        String expected = new String(buffer, 0, read, "utf-8").trim();
        assertEquals("Write", expected, result);
    }

}
