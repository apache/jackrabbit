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

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeDefinition;

public class XMLQNodeDefinition extends XMLQItemDefinition
        implements QNodeDefinition {

    private final NodeDef def;

    public XMLQNodeDefinition(NodeDef def) {
        super(def);
        this.def = def;
    }

    //-----------------------------------------------------< QNodeDefinition >

    public boolean allowsSameNameSiblings() {
        return def.allowsSameNameSiblings();
    }

    public QName getDefaultPrimaryType() {
        return def.getDefaultPrimaryType();
    }

    public QName[] getRequiredPrimaryTypes() {
        return def.getRequiredPrimaryTypes();
    }

}
