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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemStateManager;

import java.io.IOException;

import javax.jcr.RepositoryException;

/**
 * Common base class for errors detected during the consistency check.
 */
public abstract class ConsistencyCheckError {

    /**
     * Diagnostic message for this error.
     */
    protected final String message;

    /**
     * The id of the affected node.
     */
    protected final NodeId id;

    ConsistencyCheckError(String message, NodeId id) {
        this.message = message;
        this.id = id;
    }

    /**
     * Returns the diagnostic message.
     * @return the diagnostic message.
     */
    public String toString() {
        return message;
    }

    /**
     * Returns <code>true</code> if this error can be repaired.
     * @return <code>true</code> if this error can be repaired.
     */
    public abstract boolean repairable();

    /**
     * Executes the repair operation.
     * @throws Exception if an error occurs while repairing.
     */
    abstract void repair() throws Exception;

    /**
     * Double check the error. Used to rule out false positives in live environments.
     *
     * @return <code>true</code> if the error was verified to still exist, else <code>false</code>.
     * @throws RepositoryException
     * @throws IOException
     */
    abstract boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
            throws RepositoryException, IOException;
}
