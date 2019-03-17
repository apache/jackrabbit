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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * JCR command Exception
 */
public class CommandException extends Exception {
    /** Resource bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /**
     * Exception arguments
     */
    private Object[] arguments;

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3978426922931860275L;

    /**
     * @param message
     *        the message
     */
    public CommandException(String message) {
        super(message);
    }

    /**
     * @param message
     *        the message
     * @param arguments
     *        the arguments
     */
    public CommandException(String message, Object[] arguments) {
        super(message);
        this.arguments = arguments;
    }

    /**
     * @param message
     *        the message
     * @param cause
     *        the cause
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     *        the message
     * @param cause
     *        the cause
     * @param arguments
     *        the arguments
     */
    public CommandException(String message, Throwable cause, Object[] arguments) {
        super(message, cause);
        this.arguments = arguments;
    }

    /**
     * @return the localized message
     */
    public String getLocalizedMessage() {
        try {
            if (this.arguments == null) {
                return bundle.getString(this.getMessage());
            } else {
                MessageFormat f = new MessageFormat("");
                f.applyPattern(bundle.getString(this.getMessage()));
                return f.format(this.arguments);
            }
        } catch (MissingResourceException e) {
            return this.getMessage();
        }
    }
}
