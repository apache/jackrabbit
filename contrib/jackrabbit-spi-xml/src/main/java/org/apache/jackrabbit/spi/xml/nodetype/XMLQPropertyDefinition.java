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
package org.apache.jackrabbit.spi.xml.nodetype;

import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.value.QValueFactoryImpl;

public class XMLQPropertyDefinition extends XMLQItemDefinition
        implements QPropertyDefinition {

    private final PropDef def;

    public XMLQPropertyDefinition(PropDef def) {
        super(def);
        this.def = def;
    }

    private QValue getQValue(InternalValue value) throws RepositoryException {
        QValueFactory factory = QValueFactoryImpl.getInstance();
        Object object = value.internalValue();
        switch (value.getType()) {
        case PropertyType.BINARY:
            return factory.create(
                    ((BLOBFileValue) object).getString(), PropertyType.BINARY);
        case PropertyType.DATE:
            return factory.create((Calendar) object);
        case PropertyType.NAME:
            return factory.create((QName) object);
        case PropertyType.PATH:
            return factory.create((Path) object);
        case PropertyType.REFERENCE:
        case PropertyType.BOOLEAN:
        case PropertyType.DOUBLE:
        case PropertyType.LONG:
        case PropertyType.STRING:
            return factory.create(object.toString(), value.getType());
        }
        throw new RepositoryException("Unknown value type: " + value.getType());
    }

    //-------------------------------------------------< QPropertyDefinition >

    public QValue[] getDefaultValues() {
        try {
            InternalValue[] values = def.getDefaultValues();
            QValue[] qvalues = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                qvalues[i] = getQValue(values[i]);
            }
            return qvalues;
        } catch (RepositoryException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.QPropertyDefinition#getRequiredType()
     */
    public int getRequiredType() {
        return def.getRequiredType();
    }

    public String[] getValueConstraints() {
        ValueConstraint[] constraints = def.getValueConstraints();
        String[] strings = new String[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            strings[i] = constraints[i].getDefinition();
        }
        return strings;
    }

    public boolean isMultiple() {
        return def.isMultiple();
    }

}
