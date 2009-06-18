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
import org.apache.jackrabbit.value.DateValue;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.util.Calendar;
import java.util.Date;

/**
 * <code>DateConstraintTest</code>...
 */
public class DateConstraintTest extends ValueConstraintTest {

    private static Logger log = LoggerFactory.getLogger(DateConstraintTest.class);

    protected int getType() {
        return PropertyType.DATE;
    }

    protected String[] getInvalidQDefinitions() {
        return new String[] {"abc", "-18", "1234567"};
    }

    protected String[] getDefinitions() throws RepositoryException {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(234567);

        Calendar c2 = Calendar.getInstance();
        c2.setTime(new Date());

        StringBuffer b = new StringBuffer("(");
        b.append(new DateValue(c1).getString());
        b.append(",");
        b.append(new DateValue(c2).getString());
        b.append(")");

        return new String[] {b.toString()};
    }

    protected String[] getQDefinitions() throws RepositoryException {
        return getDefinitions();
    }

    protected QValue[] createNonMatchingValues() throws RepositoryException {
        Calendar cd = Calendar.getInstance();
        cd.setTimeInMillis(997);
        return new QValue[] {valueFactory.create(cd)};
    }

    protected QValue createOtherValueType() throws RepositoryException {
        return valueFactory.create(resolver.getQName("abc"));
    }

    // TODO: add more
}
