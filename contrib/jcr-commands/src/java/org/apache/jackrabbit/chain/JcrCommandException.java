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
package org.apache.jackrabbit.chain;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * JCR command Exception
 */
public class JcrCommandException extends Exception
{
    /** Resource bundle */
    private ResourceBundle bundle = ResourceBundle.getBundle(this.getClass()
        .getPackage().getName()
            + ".exceptions");

    private Object[] arguments;

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3978426922931860275L;

    /**
     * @param message
     */
    public JcrCommandException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param arguments
     */
    public JcrCommandException(String message, Object[] arguments)
    {
        super(message);
        this.arguments = arguments;
    }

    /**
     * @param message
     * @param cause
     */
    public JcrCommandException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param arguments
     */
    public JcrCommandException(String message, Throwable cause,
        Object[] arguments)
    {
        super(message, cause);
        this.arguments = arguments;
    }

    public String getLocalizedMessage()
    {
        try
        {
            if (this.arguments == null)
            {
                return bundle.getString(this.getMessage());
            } else
            {
                MessageFormat f = new MessageFormat("");
                f.applyPattern(bundle.getString(this.getMessage()));
                return f.format(this.arguments);
            }
        } catch (MissingResourceException e)
        {
            return this.getMessage();
        }
    }
}
