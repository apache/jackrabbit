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
package org.apache.jackrabbit.server.remoting.davex;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class JsonWriterTest extends EasyMockSupport {

    @Test
    public void testDoubleOutput() throws Exception {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);

        Node parent = createMock(Node.class);
        Property doubleProperty = createMock(Property.class);
        Value doublePropertyValue = createMock(Value.class);
        expect(doubleProperty.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
        expect(doubleProperty.getName()).andReturn("singleValued").anyTimes();
        expect(doubleProperty.isMultiple()).andReturn(false).anyTimes();
        expect(doubleProperty.getValue()).andReturn(doublePropertyValue).anyTimes();
        expect(doublePropertyValue.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
        expect(doublePropertyValue.getDouble()).andReturn(5d).anyTimes();
        expect(doublePropertyValue.getString()).andReturn("5").anyTimes();

        Property mvDoubleProperty = createMock(Property.class);
        Value mvDoublePropertyValue1 = createMock(Value.class);
        Value mvDoublePropertyValue2 = createMock(Value.class);
        expect(mvDoubleProperty.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
        expect(mvDoubleProperty.getName()).andReturn("multiValued").anyTimes();
        expect(mvDoubleProperty.isMultiple()).andReturn(true).anyTimes();
        expect(mvDoubleProperty.getValues()).andReturn(new Value[] { mvDoublePropertyValue1, mvDoublePropertyValue2}).anyTimes();
        expect(mvDoublePropertyValue1.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
        expect(mvDoublePropertyValue1.getDouble()).andReturn(42d).anyTimes();
        expect(mvDoublePropertyValue1.getString()).andReturn("42").anyTimes();
        expect(mvDoublePropertyValue2.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
        expect(mvDoublePropertyValue2.getDouble()).andReturn(98.6).anyTimes();
        expect(mvDoublePropertyValue2.getString()).andReturn("98.6").anyTimes();

        final List<Property> properties = new ArrayList<Property>();
        properties.add(doubleProperty);
        properties.add(mvDoubleProperty);
        expect(parent.getProperties()).andAnswer(new IAnswer<PropertyIterator>() {
            @Override
            public PropertyIterator answer() throws Throwable {
                return new PropertyIteratorAdapter(properties.iterator());
            }
        });
        expect(parent.getNodes()).andAnswer(new IAnswer<NodeIterator>() {
            @Override
            public NodeIterator answer() throws Throwable {
                return new NodeIteratorAdapter(Collections.<Node>emptyIterator());
            }
        });
        replayAll();

        jsonWriter.write(parent, 1);

        assertEquals("{\":singleValued\":\"Double\",\"singleValued\":5,\":multiValued\":\"Double\",\"multiValued\":[42,98.6],\"::NodeIteratorSize\":0}",
                writer.toString());

        verifyAll();
    }

}