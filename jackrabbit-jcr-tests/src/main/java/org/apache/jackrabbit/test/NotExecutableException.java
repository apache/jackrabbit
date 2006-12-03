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
package org.apache.jackrabbit.test;

/**
 * This exception indicates that a test case cannot be executed due to missing
 * data or if the repository does not have the ability to perform the test. E.g.
 * a feature is optional.
 * <p>
 * A test method may simply declare this exception in its signature and throw
 * this exception at any point in the method.<br/>
 * The TCK framework will take care that a test method throwing this exception
 * is not considered to be in error, but that the repository is unable to
 * execute this test.
 */
public class NotExecutableException extends Exception {

    /**
     * Creates a <code>NotExecutableException</code> without a message.
     */
    public NotExecutableException() {
    }

    /**
     * Creates a <code>NotExecutableException</code> with a detailed message.
     * @param message describes why the test case cannot be executed.
     */
    public NotExecutableException(String message) {
        super(message);
    }
}
