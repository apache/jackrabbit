/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.rmi.value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.rmi.value.SerialValueFactory;

import junit.framework.TestCase;

/**
 * TODO
 */
public class SerialValueFactoryTest extends TestCase {

    private static final String TEST_STRING = "test string";

    public void testStringValueSerialization() throws Exception {
        ValueFactory factory = SerialValueFactory.getInstance();
        Value value = factory.createValue(TEST_STRING);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        output.writeObject(value);
        output.close();

        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(buffer.toByteArray()));
        Value copy = (Value) input.readObject();

        assertEquals(copy.getString(), TEST_STRING);
    }

    public void testBinaryValueSerialization() throws Exception {
        ValueFactory factory = SerialValueFactory.getInstance();
        Value value = factory.createValue(
                new ByteArrayInputStream(TEST_STRING.getBytes()));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        output.writeObject(value);
        output.close();

        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(buffer.toByteArray()));
        Value copy = (Value) input.readObject();

        assertEquals(copy.getString(), TEST_STRING);
    }

}
