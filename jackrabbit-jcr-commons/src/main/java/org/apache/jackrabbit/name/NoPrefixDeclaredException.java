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

/**
 * Thrown when the namespace prefix of a qualified name is not found. This
 * exception is thrown when trying to convert a qualified name whose namespace
 * prefix is not found into a JCR name string. The JCR name string can not be
 * created without the namespace prefix.
 *
 * @deprecated
 */
public class NoPrefixDeclaredException extends NameException {

    /**
     * Creates a NoPrefixDeclaredException with the given error message.
     *
     * @param message error message
     */
    public NoPrefixDeclaredException(String message) {
        super(message);
    }

    /**
     * Creates a NoPrefixDeclaredException with the given error message and
     * root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public NoPrefixDeclaredException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
