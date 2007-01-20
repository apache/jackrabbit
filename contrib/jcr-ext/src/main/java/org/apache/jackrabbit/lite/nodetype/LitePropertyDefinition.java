/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite.nodetype;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.base.nodetype.BasePropertyDefinition;

public class LitePropertyDefinition extends BasePropertyDefinition
        implements PropertyDefinition {

    private NodeType declaringNodeType;

    private int requiredType;

    private int onParentVersion;

    private boolean multiple;

    private boolean autoCreated;

    private boolean mandatory;

    private boolean _protected;

    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    protected void setDeclaringNodeType(NodeType declaringNodeType) {
        this.declaringNodeType = declaringNodeType;
    }

    public int getRequiredType() {
        return requiredType;
    }

    protected void setRequiredType(int requiredType) {
        this.requiredType = requiredType;
    }

    public int getOnParentVersion() {
        return onParentVersion;
    }

    protected void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    public boolean isMultiple() {
        return multiple;
    }

    protected void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isAutoCreated() {
        return autoCreated;
    }

    protected void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    protected void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isProtected() {
        return _protected;
    }

    protected void setProtected(boolean _protected) {
        this._protected = _protected;
    }
    
}
