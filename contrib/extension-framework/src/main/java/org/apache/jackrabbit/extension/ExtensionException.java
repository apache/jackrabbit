/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.extension;

/**
 * The <code>ExtensionException</code> class defines an exception which may be
 * thrown in the Jackrabbit Extension Framework.
 *
 * @author Felix Meschberger
 */
public class ExtensionException extends Exception {

    /**
     * serialization identification
     */
    private static final long serialVersionUID = 535080559025771531L;

    /**
     * Creates an instance of this exception with a message.
     *
     * @param message The message describing the problem.
     */
    public ExtensionException(String message) {
        super(message);
    }

    /**
     * Creates an instance of this exception with causing <code>Throwable</code>.
     *
     * @param cause The <code>Throwable</code> causing the problem.
     */
    public ExtensionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance of this exception with a message and a causing
     * <code>Throwable</code>.
     *
     * @param message The message describing the problem.
     * @param cause The <code>Throwable</code> causing the problem.
     */
    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
