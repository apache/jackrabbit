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

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;

/**
 * <code>PathConstraintTest</code>...
 */
public class PathConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(PathConstraintTest.class);

    protected int getType() {
        return PropertyType.PATH;
    }

    protected String[] getInvalidQDefinitions() throws NamespaceException, IllegalNameException, MalformedPathException {
        return new String[] {"12345", "*"};
    }

    protected String[] getDefinitions() throws RepositoryException {
        return new String[] {"/abc/*", "/", "abc/*", "/*", "/abc/def"};
    }

    protected String[] getQDefinitions() throws RepositoryException {
        return new String[] {
                resolver.getQPath("/abc").getString() + PathConstraint.WILDCARD,
                resolver.getQPath("/").getString(),
                resolver.getQPath("abc").getString() + PathConstraint.WILDCARD,
                PathConstraint.WILDCARD,
                resolver.getQPath("/abc/def").getString()};
    }

    protected QValue[] createNonMatchingValues() throws RepositoryException {
        QValue root = valueFactory.create(resolver.getQPath("/"));
        QValue abs = valueFactory.create(resolver.getQPath("/uvw/xyz"));
        QValue rel = valueFactory.create(resolver.getQPath("uvw/xyz"));
        return new QValue[] {abs,abs,rel,root,abs};
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(23);
    }

    public void testGetDefinition() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (int i = 0; i < qDefs.length; i++) {
            ValueConstraint vc = createValueConstraint(qDefs[i]);
            String jcrConstraint = vc.getDefinition(resolver);

            assertFalse(qDefs[i].equals(jcrConstraint));
            assertTrue(getDefinitions()[i].equals(jcrConstraint));
        }
    }
}
