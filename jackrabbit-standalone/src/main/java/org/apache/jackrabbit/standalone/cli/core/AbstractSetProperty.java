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
package org.apache.jackrabbit.standalone.cli.core;

import org.apache.commons.chain.Command;

/**
 * SetProperty superclass.
 */
public abstract class AbstractSetProperty implements Command {
    // ---------------------------- < keys >

    /** destination node path key */
    protected String parentPathKey = "parentPath";

    /** Property name key */
    protected String nameKey = "name";

    /** Propety type key */
    protected String typeKey = "type";

    /** Property value key */
    protected String valueKey = "value";

    /**
     * @return Returns the valueKey.
     */
    public String getValueKey() {
        return valueKey;
    }

    /**
     * @param valueKey
     *        Set the context attribute key for the value attribute.
     */
    public void setValueKey(String valueKey) {
        this.valueKey = valueKey;
    }

    /**
     * @return the parent path key
     */
    public String getParentPathKey() {
        return parentPathKey;
    }

    /**
     * Sets the parent path key
     * @param parentPathKey
     *        the parent path key
     */
    public void setParentPathKey(String parentPathKey) {
        this.parentPathKey = parentPathKey;
    }

    /**
     * @return the name key
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * Sets the name key
     * @param nameKey
     *        the name key
     */
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    /**
     * @return the type key
     */
    public String getTypeKey() {
        return typeKey;
    }

    /**
     * Sets the type key
     * @param typeKey
     *        the type key
     */
    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }
}
