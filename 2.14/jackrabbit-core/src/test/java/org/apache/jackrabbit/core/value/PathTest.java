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
package org.apache.jackrabbit.core.value;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>PathTest</code>...
 */
public class PathTest extends AbstractJCRTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(PathTest.class);

    Property prop;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prop = testRootNode.setProperty(propertyName1, "/a/b/c", PropertyType.PATH);
    }

    public void testGetBinary() throws RepositoryException, IOException {
        Binary binary = prop.getBinary();
        byte[] bytes = new byte[(int) binary.getSize()];
        binary.read(bytes, 0);
        binary.dispose();

        assertEquals(prop.getString(), new String(bytes, "UTF-8"));
    }

    public void testGetBinaryFromValue() throws RepositoryException, IOException {
        Value v = superuser.getValueFactory().createValue("/a/b/c", PropertyType.PATH);
        Binary binary = v.getBinary();

        byte[] bytes = new byte[(int) binary.getSize()];
        binary.read(bytes, 0);
        binary.dispose();

        assertEquals(prop.getString(), new String(bytes, "UTF-8"));
    }

    public void testGetStream() throws RepositoryException, IOException {
        InputStream in = prop.getStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);

        assertEquals(prop.getString(), new String(out.toByteArray(), "UTF-8"));
    }

    public void testGetStreamFromValue() throws RepositoryException, IOException {
        Value v = superuser.getValueFactory().createValue("/a/b/c", PropertyType.PATH);
        InputStream in = v.getStream();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);

        assertEquals(prop.getString(), new String(out.toByteArray(), "UTF-8"));
    }
}
