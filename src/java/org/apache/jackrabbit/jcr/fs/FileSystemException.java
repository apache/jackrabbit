/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.fs;

import org.apache.jackrabbit.jcr.core.BaseException;

/**
 * The <code>FileSystemException</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.13 $, $Date: 2004/09/01 15:14:29 $
 */
public class FileSystemException extends BaseException {
    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public FileSystemException() {
	super();
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public FileSystemException(String message) {
	super(message);
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message and root cause.
     *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public FileSystemException(String message, Throwable rootCause) {
	super(message, rootCause);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public FileSystemException(Throwable rootCause) {
	super(rootCause);
    }
}
