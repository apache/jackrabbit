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
 * Describes a journal operation for a property modification.
 */
public class PropertyModifiedOperation extends PropertyOperation {

    /**
     * Creates a new instance of this class.
     */
    PropertyModifiedOperation() {
        super(ItemOperation.MODIFIED);
    }

    /**
     * Create a property record for a modified property. Only modified/modifiable members must be transmitted.
     *
     * @param state property state
     * @return property operation
     */
    public static PropertyOperation create(PropertyState state) {
        PropertyOperation operation = new PropertyModifiedOperation();
        operation.setId(state.getPropertyId());
        //todo set other members
        return operation;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(ChangeLog changeLog) {
        PropertyState state = new PropertyState(getId(), PropertyState.STATUS_NEW, false);
        state.setStatus(PropertyState.STATUS_EXISTING_MODIFIED);
        changeLog.modified(state);
    }

}
