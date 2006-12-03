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

import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Describes a journal operation for a property deletion.
 */
public class PropertyDeletedOperation extends PropertyOperation {

    /**
     * Creates a new instance of this class.
     */
    PropertyDeletedOperation() {
        super(ItemOperation.DELETED);
    }

    /**
     * Create a property record for a deleted property. The only member that must be transmitted is the property id.
     *
     * @param state property state
     * @return property operation
     */
    public static PropertyOperation create(PropertyState state) {
        PropertyOperation operation = new PropertyDeletedOperation();
        operation.setId(state.getPropertyId());
        return operation;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(ChangeLog changeLog) {
        PropertyState state = new PropertyState(getId(), PropertyState.STATUS_NEW, false);
        state.setStatus(PropertyState.STATUS_EXISTING_REMOVED);
        changeLog.deleted(state);
    }
}