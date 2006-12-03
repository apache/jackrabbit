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
package org.apache.jackrabbit.core.query.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.core.query.XMLTextFilter;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.uuid.UUID;

public class XMLTextFilterTest extends TestCase {

    public void testCanExtractAttributes() throws Exception {
        String xml = "<config><city name=\"Stockholm\"/></config>";
        PropertyId id = new PropertyId(new NodeId(new UUID(1, 1)), new QName("", ""));
        PropertyState state = new PropertyState(id, 1, true);

        InternalValue value = InternalValue.create(xml.getBytes());
        state.setValues(new InternalValue[]{value});

        TextFilter filter = new XMLTextFilter();
        Map fields = filter.doFilter(state, System.getProperty("encoding"));
        Reader reader = (Reader)fields.get(FieldNames.FULLTEXT);
        String result = getValue(reader);
        assertEquals("Stockholm", result.trim());
    }

    public void testCanExtractCData() throws Exception {
        String xml = "<config><city>Stockholm</city></config>";
        PropertyId id = new PropertyId(new NodeId(new UUID(1, 1)), new QName("", ""));
        PropertyState state = new PropertyState(id, 1, true);

        InternalValue value = InternalValue.create(xml.getBytes());
        state.setValues(new InternalValue[]{value});

        TextFilter filter = new XMLTextFilter();
        Map fields = filter.doFilter(state, System.getProperty("encoding"));
        Reader reader = (Reader)fields.get(FieldNames.FULLTEXT);
        String result = getValue(reader);
        assertEquals("Stockholm", result.trim());
    }

    public String getValue(Reader r) throws IOException {
        StringWriter w = new StringWriter();
        char[] buf = new char[1000];
        int length = 0;
        while ((length = r.read(buf)) != -1) {
            w.write(buf, 0, length);
        }

        String result = w.getBuffer().toString();
        return result;
    }
}
