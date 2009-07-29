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

import java.util.Calendar;
import java.util.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractQValueFactory</code>...
 */
public abstract class AbstractQValueFactory implements QValueFactory {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractQValueFactory.class);


    /**
     * the default encoding
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    protected static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    protected static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();


    //------------------------------------------------------< QValueFactory >---
    /**
     * @see QValueFactory#computeAutoValues(org.apache.jackrabbit.spi.QPropertyDefinition)
     */
    public QValue[] computeAutoValues(QPropertyDefinition propertyDefinition) throws RepositoryException {
        final String userId = "undefined";

        Name declaringNT = propertyDefinition.getDeclaringNodeType();
        Name name = propertyDefinition.getName();

        if (NameConstants.JCR_UUID.equals(name)
                && NameConstants.MIX_REFERENCEABLE.equals(declaringNT)) {
            // jcr:uuid property of a mix:referenceable
            return new QValue[]{create(UUID.randomUUID().toString(), PropertyType.STRING)};

        } else {
            throw new RepositoryException("createFromDefinition not implemented for: " + name);
        }
    }
}