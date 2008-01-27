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
package org.apache.jackrabbit.ocm.exception;


/**
 * Occurs when it is not possible to persist an object
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class ObjectContentManagerException extends NestableRuntimeException {
    /** Use serialVersionUID for interoperability. */
    private final static long serialVersionUID = -643089296035094427L;

    /**
     * Not intended for usage.
     */
    protected ObjectContentManagerException() {
    }

    /**
     * Constructor with message.
     *
     * @param message the message associated to the exception
     */
    public ObjectContentManagerException(String message) {
        super(message);
    }

    /**
     * Constructor with throwable object.
     *
     * @param nested the associated throwable object
     */
    public ObjectContentManagerException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructor with message and throwable object.
     *
     * @param message the message associated to the exception
     * @param nested the associated throwable object
     */
    public ObjectContentManagerException(String message, Throwable nested) {
        super(message, nested);
    }
}