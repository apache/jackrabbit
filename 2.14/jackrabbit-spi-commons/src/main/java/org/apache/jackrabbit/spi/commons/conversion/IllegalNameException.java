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
package org.apache.jackrabbit.spi.commons.conversion;

/**
 * Thrown when an illegal JCR name string is encountered. This exception is
 * thrown when attempting to parse a JCR name string that does not match the
 * JCR name syntax, or is otherwise not a legal name. Note that an
 * {@link javax.jcr.NamespaceException} is thrown if the prefix of the JCR name
 * string is syntactically valid but not bound to any namespace.
 * <p>
 * See the section 4.6 of the JCR 1.0 specification for details of the
 * JCR name syntax.
 */
public class IllegalNameException extends NameException {

    /**
     * Creates an IllegalNameException with the given error message.
     *
     * @param message error message
     */
    public IllegalNameException(String message) {
        super(message);
    }

    /**
     * Creates an IllegalNameException with the given error message and
     * root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public IllegalNameException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
