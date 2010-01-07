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
package org.apache.jackrabbit.standalone.cli;

import java.util.ResourceBundle;

import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Command Line parameter superclass
 */
public abstract class AbstractParameter implements Cloneable {

    /** Resource bundle */
    protected ResourceBundle bundle = CommandHelper.getBundle();

    /** name */
    private String name;

    /** long name */
    private String longName;

    /** description */
    private String description;

    /** command or a context attribute */
    private String contextKey;

    /** value */
    private String value;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the localized description
     */
    public abstract String getLocalizedDescription();

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param description
     *        The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param name
     *        the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *        The value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the longName
     */
    public String getLongName() {
        return longName;
    }

    /**
     * @param longName
     *        The longName to set
     */
    public void setLongName(String longName) {
        this.longName = longName;
    }

    /**
     * @param param
     *        the <code>AbstractParameter</code> to clone
     */
    protected void clone(AbstractParameter param) {
        param.contextKey = this.contextKey;
        param.description = this.description;
        param.longName = this.longName;
        param.name = this.name;
        param.value = this.value;
    }

    /**
     * @return the commandAttribute. if the context key is unset it returns the
     *         parameter name
     */
    public String getContextKey() {
        if (contextKey == null) {
            return this.name;
        }
        return contextKey;
    }

    /**
     * @param commandAttribute
     *        The commandAttribute to set
     */
    public void setContextKey(String commandAttribute) {
        this.contextKey = commandAttribute;
    }

    /**
     * @return true if this parameter is mandatory
     */
    public abstract boolean isRequired();

    /**
     * @return argumentlocalized name
     */
    public abstract String getLocalizedArgName();

}
