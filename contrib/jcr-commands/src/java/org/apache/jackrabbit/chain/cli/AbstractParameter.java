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
package org.apache.jackrabbit.chain.cli;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Command Line parameter superclass
 */
public abstract class AbstractParameter implements Cloneable
{

    /** Resource bundle */
    protected ResourceBundle bundle = ResourceBundle.getBundle(this.getClass()
        .getPackage().getName()
            + ".resources");

    /** name */
    private String name;

    /** long name */
    private String longName;

    /** description */
    private String description;

    /** command or a context attribute */
    private String commandAttribute;

    /** value */
    private String value;

    /**
     * @return Returns the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return localized description
     */
    public String getLocalizedDescription()
    {
        String str = null;
        if (description == null)
        {
            try
            {
                str = bundle.getString(name);
            } catch (MissingResourceException e)
            {
                str = name;
            }
        } else
        {
            try
            {
                str = bundle.getString(description);
            } catch (MissingResourceException e)
            {
                str = description;
            }
        }
        return str;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the value.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value
     *            The value to set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return Returns the longName.
     */
    public String getLongName()
    {
        return longName;
    }

    /**
     * @param longName
     *            The longName to set.
     */
    public void setLongName(String longName)
    {
        this.longName = longName;
    }

    protected void clone(AbstractParameter param)
    {
        param.commandAttribute = this.commandAttribute;
        param.description = this.description;
        param.longName = this.longName;
        param.name = this.name;
        param.value = this.value;
    }

    /**
     * @return Returns the commandAttribute.
     */
    public String getCommandAttribute()
    {
        return commandAttribute;
    }

    /**
     * @param commandAttribute
     *            The commandAttribute to set.
     */
    public void setCommandAttribute(String commandAttribute)
    {
        this.commandAttribute = commandAttribute;
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
