/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.orm.ojb;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.jackrabbit.core.InternalValue;
import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

/**
 * <p> Helper class to convert multi-valued properties into an encoded
 * string stored in a single database column.</p>
 */
public class ValuesToStringFieldConversion
    implements FieldConversion {

    private int type;

    public ValuesToStringFieldConversion() {
    }

    public ValuesToStringFieldConversion(int type) {
        this.type = type;
    }

    public Object javaToSql(Object object) throws ConversionException {
        InternalValue[] values = (InternalValue[]) object;
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i < values.length; i++) {
            buffer.append(values[i].toString());
            if (i < values.length - 1) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    public Object sqlToJava(Object object) throws ConversionException {
        ArrayList valueList = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer((String) object, ",");
        while (tokenizer.hasMoreTokens()) {
            InternalValue curValue = InternalValue.valueOf(tokenizer.nextToken(), type);
            valueList.add(curValue);
        }
        return (InternalValue[]) valueList.toArray(new InternalValue[valueList.size()]);
    }
}
