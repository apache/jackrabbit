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

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;

/**
 * <code>NameConstraintTest</code>...
 */
public class NameConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(NameConstraintTest.class);

    protected int getType() {
        return PropertyType.NAME;
    }

    protected String[] getInvalidQDefinitions() {
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
        QValue v = valueFactory.create(resolver.getQName("xyz"));
        return new QValue[] {v, v, v};
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(resolver.getQPath("xyz"));
    }

    public void testGetDefinition() throws RepositoryException {
        String[] qDefs = getQDefinitions();
        for (String qDef : qDefs) {
            ValueConstraint vc = createValueConstraint(qDef);
            String jcrConstraint = vc.getDefinition(resolver);
            assertFalse(qDef.equals(jcrConstraint));
            assertEquals(resolver.getJCRName(ValueConstraint.NAME_FACTORY.create(qDef)), jcrConstraint);
        }
    }
}
