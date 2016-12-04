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
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import junit.framework.TestCase;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

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
        List<String> l = new ArrayList<String>();
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

    public void testDecimal() throws RepositoryException {
        BigDecimal bd = new BigDecimal(Double.MIN_VALUE);

        Value v = vFactory.createValue(bd);
        QValue qv = qvFactory.create(bd);

        assertEquals(v, ValueFormat.getJCRValue(qv, resolver, vFactory));
        assertEquals(qv, ValueFormat.getQValue(v, resolver, qvFactory));
    }

    public void testURI() throws RepositoryException, URISyntaxException {
        URI uri = new URI("http://jackrabbit.apache.org");

        Value v = vFactory.createValue("http://jackrabbit.apache.org", PropertyType.URI);
        QValue qv = qvFactory.create(uri);

        assertEquals(v, ValueFormat.getJCRValue(qv, resolver, vFactory));
        assertEquals(qv, ValueFormat.getQValue(v, resolver, qvFactory));
        assertEquals(qv, ValueFormat.getQValue("http://jackrabbit.apache.org", PropertyType.URI, resolver, qvFactory));
    }

    public void testWeakReferences() throws RepositoryException {
        String reference = UUID.randomUUID().toString();

        Value v = vFactory.createValue(reference, PropertyType.WEAKREFERENCE);
        QValue qv = qvFactory.create(reference, PropertyType.WEAKREFERENCE);

        assertEquals(v, ValueFormat.getJCRValue(qv, resolver, vFactory));
        assertEquals(qv, ValueFormat.getQValue(v, resolver, qvFactory));
        assertEquals(qv, ValueFormat.getQValue(reference, PropertyType.WEAKREFERENCE, resolver, qvFactory));        
    }

    public void testGetJCRString() throws RepositoryException, URISyntaxException {
        List<QValue> qvs = new ArrayList<QValue>();

        String reference = UUID.randomUUID().toString();
        qvs.add(qvFactory.create(reference, PropertyType.WEAKREFERENCE));
        qvs.add(qvFactory.create(reference, PropertyType.REFERENCE));
        qvs.add(qvFactory.create("anyString", PropertyType.STRING));
        qvs.add(qvFactory.create(true));
        qvs.add(qvFactory.create(12345));
        qvs.add(qvFactory.create(12345.7889));
        qvs.add(qvFactory.create(new URI("http://jackrabbit.apache.org")));
        qvs.add(qvFactory.create(new BigDecimal(Double.MIN_VALUE)));
        qvs.add(qvFactory.create(new byte[] {'a','b','c'}));
        qvs.add(qvFactory.create(NameConstants.JCR_ACTIVITIES));
        qvs.add(ValueFormat.getQValue("/a/b/c", PropertyType.PATH, resolver, qvFactory));

        for (QValue qv : qvs) {
            assertEquals(ValueFormat.getJCRValue(qv, resolver, vFactory).getString(), ValueFormat.getJCRString(qv, resolver));
        }
    }
}