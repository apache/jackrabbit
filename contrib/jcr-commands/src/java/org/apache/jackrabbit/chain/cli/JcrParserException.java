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

import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Exception thrown if any error occurs while parsing the user's input.
 */
public class JcrParserException extends JcrCommandException
{

    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3761694498056713525L;

    /**
     * @param message
     * @param arguments
     */
    public JcrParserException(String message, Object[] arguments)
    {
        super(message, arguments);
    }

    /**
     * @param message
     * @param cause
     * @param arguments
     */
    public JcrParserException(String message, Throwable cause,
        Object[] arguments)
    {
        super(message, cause, arguments);
    }

    /**
     * @param message
     */
    public JcrParserException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public JcrParserException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
