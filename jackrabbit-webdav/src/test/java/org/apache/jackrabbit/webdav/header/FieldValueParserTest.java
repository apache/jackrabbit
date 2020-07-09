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
package org.apache.jackrabbit.webdav.header;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

import junit.framework.TestCase;

public class FieldValueParserTest extends TestCase {

    @Test
    public void testDavComplianceHeader() {

        List<String> l;

        l = FieldValueParser.tokenizeList("1");
        assertArrayEquals(new String[]{"1"}, l.toArray());

        l = FieldValueParser.tokenizeList("1,2,,,,,3,,bind,");
        assertArrayEquals(new String[]{"1","2","3","bind"}, l.toArray());

        l = FieldValueParser.tokenizeList("1,2,<http://example.com/foo,bar>");
        assertArrayEquals(new String[]{"1","2","<http://example.com/foo,bar>"}, l.toArray());
    }
}
