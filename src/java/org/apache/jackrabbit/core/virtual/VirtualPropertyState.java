/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.virtual;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.PropertyState;

/**
 * This Class implements a virtual property state
 */
public class VirtualPropertyState extends PropertyState {

    /**
     * Creates a new virtual property state
     * @param name
     * @param parentUUID
     */
    public VirtualPropertyState(QName name, String parentUUID) {
        super(name, parentUUID, ItemState.STATUS_EXISTING, false);
    }
}
