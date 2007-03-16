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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.value.QValueFactoryImpl;

public class XMLPrimaryTypeInfo implements PropertyInfo {

    private final PropertyId id;

    public XMLPrimaryTypeInfo(PropertyId id) {
        this.id = id;
    }

    //---------------------------------------------------------< PropertyInfo>

    public boolean denotesNode() {
        return false;
    }

    public Path getPath() {
        // TODO
        return null;
    }

    public PropertyId getId() {
        return id;
    }

    public NodeId getParentId() {
        return id.getParentId();
    }

    public QName getQName() {
        return QName.JCR_PRIMARYTYPE;
    }

    public int getType() {
        return PropertyType.NAME;
    }

    public QValue[] getValues() {
        QValueFactory factory = QValueFactoryImpl.getInstance();
        return new QValue[] { factory.create(QName.NT_UNSTRUCTURED) };
    }

    public boolean isMultiValued() {
        return false;
    }

}
