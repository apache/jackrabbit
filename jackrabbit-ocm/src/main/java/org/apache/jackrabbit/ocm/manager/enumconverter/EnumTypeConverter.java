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
package org.apache.jackrabbit.ocm.manager.enumconverter;

import java.io.InvalidClassException;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;


/**
*
* SimpleEnumerationTypeConverter class.
*
* This converter can map type Enum (java.lang.Enum) to JCR properties and back.
*
* @author <a href="mailto:boni.g@bioimagene.com">Boni Gopalan</a>
*/
public class EnumTypeConverter implements AtomicTypeConverter {
    
	public Object getObject(Value value) {
        try {
            String propertyValue = value.getString();
            String[] enumerationDef = StringUtils.split(propertyValue, ':');
            String enumerationClass = enumerationDef[0];
            String enumerationValue = enumerationDef[1];
            Enum[] enumerations = (Enum[]) Class.forName(enumerationClass)
                                                .getEnumConstants();
            int size = enumerations.length;

            for (int i = 0; i < size; i++) {
                if (enumerations[i].name().equals(enumerationValue)) {
                    return enumerations[i];
                }
            }

            throw new RuntimeException(new InvalidClassException(enumerationClass +
                    " Does not contain an enumeration " + enumerationValue));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Value getValue(ValueFactory valueFactory, Object object) {
        if (object == null) {
            return null;
        }

        if (!(object instanceof Enum)) {
            throw new RuntimeException(new InvalidClassException(EnumTypeConverter.class.getSimpleName() +
                    " Can only convert simple Enumerations"));
        }

        String value;
        Enum anEnum = (Enum) (object);
        value = anEnum.getDeclaringClass().getName() + ":" + anEnum.name();

        return valueFactory.createValue(value);
    }

    public String getXPathQueryValue(ValueFactory valueFactory, Object object) {
        Value value = getValue(valueFactory, object);

        try {
            return "'" + value.getString() + "'";
        } catch (Exception e) {
            throw new RuntimeException(e.fillInStackTrace());
        }
    }
}
