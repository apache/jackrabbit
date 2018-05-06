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
package org.apache.jackrabbit.spi.commons.nodetype.compact;


import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import junit.framework.TestCase;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefDiff;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;

public class CompactNodeTypeDefTest extends TestCase {

    private static final String TEST_FILE = "cnd-reader-test-input.cnd";

    public void testCompactNodeTypeDef() throws Exception {

        // Read in node type def from test file
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(TEST_FILE));
        CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> cndReader =
            new CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(
                reader, TEST_FILE, new QDefinitionBuilderFactory());

        List<QNodeTypeDefinition> ntdList1 = cndReader.getNodeTypeDefinitions();
        NamespaceMapping nsm = cndReader.getNamespaceMapping();
        NamePathResolver resolver = new DefaultNamePathResolver(nsm);

        // Put imported node type def back into CND form with CND writer
        StringWriter sw = new StringWriter();
        CompactNodeTypeDefWriter.write(ntdList1, nsm, resolver, sw);

        // Rerun the reader on the product of the writer
        cndReader = new CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(
                new StringReader(sw.toString()), TEST_FILE, new QDefinitionBuilderFactory());

        List<QNodeTypeDefinition> ntdList2 = cndReader.getNodeTypeDefinitions();

        if (ntdList1.size() == 0 || ntdList1.size() != ntdList2.size()) {
            fail("Exported node type definition was not successfully read back in");
        } else {
            for(int k = 0; k < ntdList1.size(); k++) {
                QNodeTypeDefinition ntd1 = ntdList1.get(k);
                QNodeTypeDefinition ntd2 = ntdList2.get(k);

                NodeTypeDefDiff diff = NodeTypeDefDiff.create(ntd1, ntd2);
                if (diff.isModified() && !diff.isTrivial()){
                    fail("Exported node type definition was not successfully read back in. "
                            + ntd2.getName() + "differs from original");
                }
            }
        }
    }
}
