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

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>DateConstraintTest</code>...
 */
public class ReferenceConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(ReferenceConstraintTest.class);

    protected int getType() {
        return PropertyType.REFERENCE;
    }

    protected String[] getInvalidQDefinitions() throws NamespaceException, IllegalNameException, MalformedPathException {
        return new String[] {"12345", "", "abc"};
    }

    protected String[] getDefinitions() throws RepositoryException {
        return new String[] {"12345", "abc", "jcr:abc"};
    }

    protected String[] getQDefinitions() throws RepositoryException {
        return new String[] {
                resolver.getQName("12345").toString(),
                resolver.getQName("abc").toString(),
                resolver.getQName("jcr:abc").toString()
        };
    }

    protected QValue[] createNonMatchingValues() throws RepositoryException {
        // TODO: reference constraints are not checked property -> not executable
        throw new ConstraintViolationException();
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(23.56);
    }

    public void testCheckNonMatchingValue() throws RepositoryException {
        // not executable
    }

    public void testGetDefinition() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            String jcrConstraint = vc.getDefinition(resolver);

            assertFalse(qDefs[i].equals(jcrConstraint));
            assertEquals(resolver.getJCRName(ValueConstraint.NAME_FACTORY.create(qDefs[i])), jcrConstraint);
        }
    }
}
