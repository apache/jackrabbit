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
package org.apache.jackrabbit.base.nodetype;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

/**
 * Item definition base class.
 */
public class BaseItemDefinition implements ItemDefinition {

    /** Not implemented. {@inheritDoc} */
    public NodeType getDeclaringNodeType() {
        throw new UnsupportedOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public String getName() {
        throw new UnsupportedOperationException();
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean isAutoCreated() {
        return false;
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean isMandatory() {
        return false;
    }

    /**
     * Always returns <code>OnParentVersionAction.IGNORE</code>.
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return OnParentVersionAction.IGNORE;
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean isProtected() {
        return false;
    }

}
