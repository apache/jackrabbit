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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>BooleanConstraintTest</code>...
 */
public class BooleanConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(BooleanConstraintTest.class);

    protected ValueConstraint createInvalidConstraint() throws RepositoryException {
        return new BooleanConstraint("test");
    }

    protected ValueConstraint createValueConstraint(String definition) throws RepositoryException {
        return new BooleanConstraint(definition);
    }

    protected int getType() {
        return PropertyType.BOOLEAN;
    }

    protected String[] getInvalidQDefinitions() {
        return new String[] {"test", "/abc/d", "12345"};
    }

    protected String[] getDefinitions() {
        return new String[] { Boolean.TRUE.toString()};
    }

    protected String[] getQDefinitions() {
        return getDefinitions();
    }

    protected QValue[] createNonMatchingValues() throws RepositoryException {
        return new QValue[] {
                valueFactory.create(Boolean.FALSE.booleanValue()),
                valueFactory.create(Boolean.TRUE.booleanValue())
        };
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(1345);
    }

    public void testTrueConstraint() throws RepositoryException, ConstraintViolationException {
        ValueConstraint vc = new BooleanConstraint(Boolean.TRUE.toString());
        vc.check(valueFactory.create(true));
        try {
            vc.check(valueFactory.create(false));
            fail();
        } catch (ConstraintViolationException e) {
            // ok
        }
    }
}
