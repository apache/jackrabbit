/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.ItemId;

/**
 * The <code>ItemStateProvider</code> interface...
 */
public interface ItemStateProvider {

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @param id
     * @return
     */
    public boolean hasItemState(ItemId id);

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public ItemState getItemStateInAttic(ItemId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @param id
     * @return
     */
    public boolean hasItemStateInAttic(ItemId id);
}
