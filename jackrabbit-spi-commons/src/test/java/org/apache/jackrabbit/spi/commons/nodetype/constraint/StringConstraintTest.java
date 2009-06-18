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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.NamespaceException;

/**
 * <code>StringConstraintTest</code>...
 */
public class StringConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(StringConstraintTest.class);

    protected ValueConstraint createInvalidConstraint() throws RepositoryException {
        return new StringConstraint("[abc");
    }

    protected ValueConstraint createValueConstraint(String definition) throws RepositoryException {
        return new StringConstraint(definition);
    }

    protected ValueConstraint createValueConstraint() throws RepositoryException {
        return new StringConstraint("[abc] constraint");
    }

    protected int getType() {
        return PropertyType.STRING;
    }

    protected String[] getInvalidQDefinitions() throws NamespaceException, IllegalNameException, MalformedPathException {
        return new String[] {"[abc"};
    }

    protected String[] getDefinitions() throws RepositoryException {
        return new String[] {"[abc] constraint", "abc"};
    }

    protected String[] getQDefinitions() throws RepositoryException {
        return getDefinitions();
    }

    protected QValue[] createNonMatchingValues() throws RepositoryException {
        QValue v = valueFactory.create("another string", PropertyType.STRING);
        return new QValue[] {v,v};
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(23);
    }

    // TODO: add more
}
