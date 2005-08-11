/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.chain.command;

import org.apache.commons.chain.Command;

/**
 * SetProperty superclass. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public abstract class AbstractSetProperty implements Command
{

    // ---------------------------- < literals >

    /** Property name */
    protected String propertyName;

    /** Propety type */
    protected String propertyType;

    /** Property value */
    protected String value;

    // ---------------------------- < keys >

    /** Property name key */
    protected String propertyNameKey;

    /** Propety type key */
    protected String propertyTypeKey;

    /** Property value key */
    protected String valueKey;

    /**
     * @return Returns the name.
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setPropertyName(String name)
    {
        this.propertyName = name;
    }

    /**
     * @return Returns the type.
     */
    public String getPropertyType()
    {
        return propertyType;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setPropertyType(String type)
    {
        this.propertyType = type;
    }

    /**
     * @return the property value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets property value
     * 
     * @param value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return Returns the nameKey.
     */
    public String getPropertyNameKey()
    {
        return propertyNameKey;
    }

    /**
     * @param nameKey
     *            Set the context attribute key for the name attribute.
     */
    public void setPropertyNameKey(String nameKey)
    {
        this.propertyNameKey = nameKey;
    }

    /**
     * @return Returns the propertyTypeKey.
     */
    public String getPropertyTypeKey()
    {
        return propertyTypeKey;
    }

    /**
     * @param propertyTypeKey
     *            Set the context attribute key for the property type  attribute.
     */
    public void setPropertyTypeKey(String propertyTypeKey)
    {
        this.propertyTypeKey = propertyTypeKey;
    }

    /**
     * @return Returns the valueKey.
     */
    public String getValueKey()
    {
        return valueKey;
    }

    /**
     * @param valueKey
     *            Set the context attribute key for the value attribute.
     */
    public void setValueKey(String valueKey)
    {
        this.valueKey = valueKey;
    }
}
