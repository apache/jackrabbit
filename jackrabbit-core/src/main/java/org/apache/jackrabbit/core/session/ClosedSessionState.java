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
package org.apache.jackrabbit.core.session;

import javax.jcr.RepositoryException;

public class ClosedSessionState implements SessionState {

    /**
     * Exception that holds the stack trace of where the session was closed.
     */
    private final Exception exception;

    public ClosedSessionState() {
        this.exception = new Exception();
    }

    /**
     * Returns <code>false</code>; the session is closed.
     *
     * @return <code>false</code>
     */
    public boolean isAlive() {
        return false;
    }

    /**
     * Throws an exception; the session is closed.
     *
     * @throws RepositoryException always thrown
     */
    public void perform(SessionOperation operation) throws RepositoryException {
        throw new RepositoryException(
                "Unable to perform " + operation + " since this session"
                + " has been closed. See the chained exception for a trace"
                + " of where the session was closed", exception);
    }

}
