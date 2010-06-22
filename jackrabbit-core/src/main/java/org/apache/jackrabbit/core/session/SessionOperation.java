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

/**
 * An operation that is performed on a JCR session. Used by the
 * {@link SessionState} interface to implement generic controls like
 * synchronization and liveness checks on all session operation.
 */
public class SessionOperation {

    private final String name;

    public SessionOperation(String name) {
        this.name = name;
    }

    /**
     * Performs this operation. The default implementation does nothing;
     * subclasses should override this method to implement custom operations.
     *
     * @throws RepositoryException if the operation fails
     */
    public void perform() throws RepositoryException {
    }

    /**
     * Returns the name of this operation.
     *
     * @return operation name
     */
    public String toString() {
        return name;
    }

}
