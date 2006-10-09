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
package org.apache.jackrabbit.ntdoc.model;

import java.util.*;

/**
 * This class implements the child node definition.
 */
public final class NodeDef
        extends ItemDef {
    /**
     * Default primary type.
     */
    private String defPrimaryType;

    /**
     * Required primary types.
     */
    private final HashSet reqPrimaryTypes;

    /**
     * Construct the item.
     */
    public NodeDef(String name) {
        super(name);
        this.reqPrimaryTypes = new HashSet();
    }

    /**
     * Return the default primary type.
     */
    public String getDefaultPrimaryType() {
        return this.defPrimaryType != null ? this.defPrimaryType : "";
    }

    /**
     * Set the default primary type.
     */
    public void setDefaultPrimaryType(String defPrimaryType) {
        this.defPrimaryType = defPrimaryType;
    }

    /**
     * Return the required primary types.
     */
    public String[] getRequiredPrimaryTypes() {
        return (String[]) this.reqPrimaryTypes.toArray(new String[this.reqPrimaryTypes.size()]);
    }

    /**
     * Set the required primary types.
     */
    public void setRequiredPrimaryTypes(String[] reqPrimaryTypes) {
        setRequiredPrimaryTypes(Arrays.asList(reqPrimaryTypes));
    }

    /**
     * Set the required primary types.
     */
    public void setRequiredPrimaryTypes(Collection reqPrimaryTypes) {
        this.reqPrimaryTypes.clear();
        this.reqPrimaryTypes.addAll(reqPrimaryTypes != null ? reqPrimaryTypes : Collections.EMPTY_LIST);
    }

    /**
     * Add required primary type value.
     */
    public void addRequiredPrimaryType(String reqPrimaryType) {
        this.reqPrimaryTypes.add(reqPrimaryType);
    }
}
