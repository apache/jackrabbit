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
package org.apache.jackrabbit.spi.commons.value;

import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.DummyNamespaceResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import junit.framework.TestCase;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.ValueFactory;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>ValueFormatTest</code>...
 */
public class ValueFormatTest extends TestCase {

    private IdentifierResolver idResolver;
    private NamePathResolver resolver;
    private QValueFactory qvFactory;
    private ValueFactory vFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        idResolver = new IdentifierResolver() {
            public Path getPath(String identifier) throws MalformedPathException {
                throw new UnsupportedOperationException();
            }

            public void checkFormat(String identifier) throws MalformedPathException {
                // nop
            }
        };

        NameResolver nResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), new DummyNamespaceResolver());
        PathResolver pResolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), nResolver, idResolver);
        resolver = new DefaultNamePathResolver(nResolver, pResolver);
        qvFactory = QValueFactoryImpl.getInstance();
        vFactory = new ValueFactoryQImpl(qvFactory, resolver);
    }

    /**
     * Path values must never be normalized.
     *
     * @throws RepositoryException
     */
    public void testGetPathQValue() throws RepositoryException {
        List<String> l = new ArrayList();
        // a non-normalized absolute path
        l.add("/a/.././b/c/.");
        // an identifier based path
        l.add("["+ UUID.randomUUID().toString()+"]");


        for (String jcrPath : l) {
            QValue qv = ValueFormat.getQValue(jcrPath, PropertyType.PATH, resolver, qvFactory);
            assertFalse(qv.getPath().isNormalized());
            assertEquals("Path values must not be normalized",jcrPath, ValueFormat.getJCRValue(qv, resolver, vFactory).getString());
        }
    }

}