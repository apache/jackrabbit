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
package org.apache.jackrabbit.spi.xml;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.value.QValueFactoryImpl;

public class XMLPropertyInfo extends XMLItemInfo implements PropertyInfo {

    public XMLPropertyInfo(XMLNodeId id) {
        super(id);
    }

    //---------------------------------------------------------< PropertyInfo>

    public PropertyId getId() {
        return id;
    }

    public int getType() {
        return PropertyType.STRING;
    }

    public QValue[] getValues() {
        QValueFactory factory = QValueFactoryImpl.getInstance();
        return new QValue[] { factory.create(id.getValue(), getType()) };
    }

    public boolean isMultiValued() {
        return false;
    }

}
