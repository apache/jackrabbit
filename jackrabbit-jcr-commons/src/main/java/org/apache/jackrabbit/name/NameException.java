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
package org.apache.jackrabbit.name;

import org.apache.jackrabbit.BaseException;

/**
 * Base class for exceptions about malformed or otherwise
 * invalid JCR names and paths.
 *
 * @deprecated Use the NameException class from
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class NameException extends BaseException {

    /**
     * Creates a NameException with the given error message.
     *
     * @param message error message
     */
    public NameException(String message) {
        super(message);
    }

    /**
     * Creates a NameException with the given error message and
     * root cause exception.
     *
     * @param message   error message
     * @param rootCause root cause exception
     */
    public NameException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
