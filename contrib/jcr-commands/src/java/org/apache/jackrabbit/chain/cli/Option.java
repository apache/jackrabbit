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

/**
 * Command Line option. An option is a pair of parameters with the following
 * pattern -[option name] [option value]
 */
public class Option extends AbstractParameter
{

    /** argument name */
    private String argName;

    /** required */
    private boolean required = false;

    /**
     * @return Returns the required.
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * @param required
     *            The required to set.
     */
    public void setRequired(boolean required)
    {
        this.required = required;
    }

    /**
     * @return Returns the argName.
     */
    public String getArgName()
    {
        return argName;
    }

    /**
     * @return localized argument name
     */
    public String getLocalizedArgName()
    {
        String str = null;
        if (argName == null)
        {
            str = this.getName();
        } else
        {
            try
            {
                str = bundle.getString(argName);
            } catch (MissingResourceException e)
            {
                str = argName;
            }
        }
        return str;
    }

    /**
     * @param argName
     *            The argName to set.
     */
    public void setArgName(String argName)
    {
        this.argName = argName;
    }

    /**
     * @inheritDoc
     */
    public Object clone()
    {
        Option o = new Option();
        this.clone(o);
        return o;
    }

    protected void clone(Option opt)
    {
        super.clone(opt);
        opt.argName = this.argName;
        opt.required = this.required;

    }
}
