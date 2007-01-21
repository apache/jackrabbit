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

import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;

public class XMLPrimaryTypeId implements PropertyId {

    private final NodeId parent;

    public XMLPrimaryTypeId(NodeId parent) {
        this.parent = parent;
    }

    //----------------------------------------------------------< PropertyId >

    public NodeId getParentId() {
        return parent;
    }

    public QName getQName() {
        return QName.JCR_PRIMARYTYPE;
    }

    public boolean denotesNode() {
        return false;
    }

    public Path getPath() {
        try {
            return Path.create(parent.getPath(), getQName(), false);
        } catch (MalformedPathException e) {
            throw new IllegalStateException("Invalid path: " + this);
        }
    }

    public String getUniqueID() {
        return null;
    }

    //--------------------------------------------------------------< Object >

    public boolean equals(Object that) {
        return that instanceof XMLPrimaryTypeId
            && parent.equals(((XMLPrimaryTypeId) that).parent);
    }

    public int hashCode() {
        return parent.hashCode() * 17 + 37;
    }

}
