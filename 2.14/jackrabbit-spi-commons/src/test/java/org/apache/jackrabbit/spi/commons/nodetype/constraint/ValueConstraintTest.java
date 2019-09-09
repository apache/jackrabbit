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
package org.apache.jackrabbit.spi.commons.nodetype.constraint;

import junit.framework.TestCase;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>ValueConstraintTest</code>...
 */
public abstract class ValueConstraintTest extends TestCase {

    private static Logger log = LoggerFactory.getLogger(ValueConstraintTest.class);

    protected QValueFactory valueFactory;
    protected NamePathResolver resolver;

    protected void setUp() throws Exception {
        super.setUp();

        valueFactory = QValueFactoryImpl.getInstance();
        resolver = new DefaultNamePathResolver(new NamespaceResolver () {
            public String getURI(String prefix) throws NamespaceException {
                return prefix;
            }
            public String getPrefix(String uri) throws NamespaceException {
                return uri;
            }
        });
    }

    protected abstract int getType();

    protected abstract String[] getInvalidQDefinitions() throws RepositoryException;

    protected abstract String[] getDefinitions() throws RepositoryException;

    protected abstract String[] getQDefinitions() throws RepositoryException;

    protected abstract QValue[] createNonMatchingValues() throws RepositoryException;

    protected abstract QValue createOtherValueType() throws RepositoryException;


    protected ValueConstraint createValueConstraint(String qDefinition) throws RepositoryException {
        return ValueConstraint.create(getType(), qDefinition);
    }

    private ValueConstraint createValueConstraint(String definition,
                                                  NamePathResolver resolver) throws RepositoryException {
        return ValueConstraint.create(getType(), definition, resolver);
    }

    public void testCreateFromNull() {
        try {
            createValueConstraint(null);
            fail("attempt to create a value constraint from null should fail.");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateValueConstraints() throws RepositoryException {
        String[] defs = getDefinitions();
        for (int i = 0; i < defs.length; i++) {
            ValueConstraint vc = createValueConstraint(defs[i], resolver);
            assertEquals(defs[i], vc.getDefinition(resolver));
        }
    }

    public void testCreateValueConstraints2() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            assertEquals(qDefs[i], vc.getString());
        }
    }

    public void testCreateInvalidValueConstraints() throws RepositoryException {
        try {
            String[] invalidQDefs = getInvalidQDefinitions();
            for (int i = 0; i < invalidQDefs.length; i++) {
                createValueConstraint(invalidQDefs[i]);
                fail("Creating an invalid definition should throw InvalidConstraintException");
            }
        } catch (InvalidConstraintException e) {
            //ok
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testGetDefinition() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            String jcrConstraint = vc.getDefinition(resolver);
            assertNotNull(jcrConstraint);
            assertEquals(qDefs[i], jcrConstraint);
        }
    }

    public void testGetString() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            assertEquals(qDefs[i], vc.getString());
        }
    }

    public void testCheckNullValue() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            try {
                vc.check(null);
                fail("ValueConstraint.check(null) should throw ConstraintViolationException.");
            } catch (ConstraintViolationException e) {
                //ok
            }
        }
    }

    public void testCheckNonMatchingValue() throws RepositoryException {
        QValue[] nonMatching = createNonMatchingValues();

        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            if (i >= nonMatching.length) {
                break;
            }

            ValueConstraint vc = createValueConstraint(qDefs[i]);
            try {
                vc.check(nonMatching[i]);
                fail("ValueConstraint.check() with non-matching value should throw ConstraintViolationException.");
            } catch (ConstraintViolationException e) {
                //ok
            }
        }
    }

    public void testCheckWrongValueType() throws RepositoryException {
        QValue val = createOtherValueType();
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            try {
                vc.check(val);
                fail("ValueConstraint.check() with non-matching value should throw ConstraintViolationException.");
            } catch (RepositoryException e) {
                //ok
            }
        }
    }

    public void testEquals() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            ValueConstraint vc2 = createValueConstraint(qDefs[i]);
            assertEquals(vc, vc2);

            vc2 = createValueConstraint(vc.getString());
            assertEquals(vc, vc2);
        }
    }
}
