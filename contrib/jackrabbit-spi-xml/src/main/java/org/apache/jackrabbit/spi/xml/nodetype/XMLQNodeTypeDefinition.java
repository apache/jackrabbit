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

import java.util.Collection;

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;

public class XMLQNodeTypeDefinition implements QNodeTypeDefinition {

    private final NodeTypeDef def;

    public XMLQNodeTypeDefinition(NodeTypeDef definition) {
        this.def = definition;
    }

    //-------------------------------------------------< QNodeTypeDefinition >

    public QNodeDefinition[] getChildNodeDefs() {
        NodeDef[] defs = def.getChildNodeDefs();
        QNodeDefinition[] definitions = new QNodeDefinition[defs.length];
        for (int i = 0; i < defs.length; i++) {
            definitions[i] = new XMLQNodeDefinition(defs[i]);
        }
        return definitions;
    }

    public Collection getDependencies() {
        return def.getDependencies();
    }

    public QName getPrimaryItemName() {
        return def.getPrimaryItemName();
    }

    public QPropertyDefinition[] getPropertyDefs() {
        PropDef[] defs = def.getPropertyDefs();
        QPropertyDefinition[] definitions = new QPropertyDefinition[defs.length];
        for (int i = 0; i < defs.length; i++) {
            definitions[i] = new XMLQPropertyDefinition(defs[i]);
        }
        return definitions;
    }

    public QName getQName() {
        return def.getName();
    }

    public QName[] getSupertypes() {
        return def.getSupertypes();
    }

    public boolean hasOrderableChildNodes() {
        return def.hasOrderableChildNodes();
    }

    public boolean isMixin() {
        return def.isMixin();
    }

}
