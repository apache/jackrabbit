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
package org.apache.jackrabbit.xsd2cnd;


import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.util.name.NamespaceMapping;
import org.apache.jackrabbit.util.name.NamespaceExtractor;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.*;

import junit.framework.TestCase;

import javax.jcr.NamespaceException;

/**
 * Tests the SchemaConverter
 *
 * Usage:
 * java SchemaConverterTest infile outfile defaultprefix
 *
 * infile - XML schema file to be converted
 * outfile - file to which a pretty print of the resulting namespace mappings and node type definitions is put.
 * defaultprefix - The prefix used in cases where the XML schema file specifies a default namespace
 */
public class SchemaConverterTest extends TestCase {

    private static Logger log = LoggerFactory.getLogger(SchemaConverterTest.class);
    private static final String TEST_INPUT_FILE = "applications/test/xsd-converter-test-input.xsd";
    private static final String MODEL_RESULT_FILE = "applications/test/xsd-converter-model-output.cnd";
    private static final String DEFAULT_PREFIX = "test";

    public void testSchemaConverter() {
        try {
            NamespaceExtractor nse = new NamespaceExtractor(TEST_INPUT_FILE, DEFAULT_PREFIX);
            NamespaceMapping nsm = nse.getNamespaceMapping();

            SchemaConverter nts = new SchemaConverter(TEST_INPUT_FILE);
            List testList = nts.getNodeTypeDefs();

            CompactNodeTypeDefReader ntr = new CompactNodeTypeDefReader(new FileReader(MODEL_RESULT_FILE), MODEL_RESULT_FILE, nsm);
            List modelList = ntr.getNodeTypeDefs();

            TreeMap orderedTestList = new TreeMap();
            TreeMap orderedModelList = new TreeMap();

            fillTreeMap(orderedTestList, testList, nsm);
            fillTreeMap(orderedModelList, modelList, nsm);

            Iterator testIterator = orderedTestList.values().iterator();
            Iterator modelIterator = orderedModelList.values().iterator();

            while(testIterator.hasNext() && modelIterator.hasNext()){
                NodeTypeDef testDef = (NodeTypeDef)testIterator.next();
                NodeTypeDef modelDef = (NodeTypeDef)modelIterator.next();
                if (!testDef.equals(modelDef)){
                    fail("test result differs from model result");
                    break;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            log.debug(msg);
        }
    }

    private void fillTreeMap(TreeMap tm, List l, NamespaceResolver nsr) throws NamespaceException {
        for(Iterator i = l.iterator(); i.hasNext();){
            NodeTypeDef ntd = (NodeTypeDef)i.next();
            String prefix = nsr.getPrefix(ntd.getName().getNamespaceURI());
            String localName = ntd.getName().getLocalName();
            tm.put(prefix + ":" + localName, ntd);
        }
    }
}
