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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Item operation interface.
 */
public abstract class ItemOperation {

    /**
     * Operation type: added.
     */
    public static final int ADDED = 1;

    /**
     * Operation type: modified.
     */
    public static final int MODIFIED = 2;

    /**
     * Operation type: deleted.
     */
    public static final int DELETED = 3;

    /**
     * Operation type.
     */
    private final int operationType;

    /**
     * Creates a new instance of this class. Takes an operation type as parameter.
     */
    protected ItemOperation(int operationType) {
        this.operationType = operationType;
    }

    /**
     * Returns the operation type.
     *
     * @return operation type
     */
    public int getOperationType() {
        return operationType;
    }

    /**
     * Apply an operation to a change log. Subclass responsibility.
     *
     * @param changeLog change log
     */
    public abstract void apply(ChangeLog changeLog);
}
