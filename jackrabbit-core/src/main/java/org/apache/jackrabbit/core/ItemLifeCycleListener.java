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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.id.ItemId;

/**
 * The <code>ItemLifeCycleListener</code> interface allows an implementing
 * object to be informed about changes on an <code>Item</code> instance.
 */
public interface ItemLifeCycleListener {

    /**
     * Called when an <code>ItemImpl</code> instance has been created.
     *
     * @param item the instance which has been created
     */
    void itemCreated(ItemImpl item);

    /**
     * Called when an <code>ItemImpl</code> instance has been invalidated
     * (i.e. it has been temporarily rendered 'invalid').
     * <p>
     * Note that most <code>{@link javax.jcr.Item}</code>,
     * <code>{@link javax.jcr.Node}</code> and <code>{@link javax.jcr.Property}</code>
     * methods will throw an <code>InvalidItemStateException</code> when called
     * on an 'invalidated' item.
     *
     * @param id   the id of the instance that has been discarded
     * @param item the instance which has been discarded
     */
    void itemInvalidated(ItemId id, ItemImpl item);

    /**
     * Called when an <code>ItemImpl</code> instance has been destroyed
     * (i.e. it has been permanently rendered 'invalid').
     * <p>
     * Note that most <code>{@link javax.jcr.Item}</code>,
     * <code>{@link javax.jcr.Node}</code> and <code>{@link javax.jcr.Property}</code>
     * methods will throw an <code>InvalidItemStateException</code> when called
     * on a 'destroyed' item.
     *
     * @param id   the id of the instance that has been destroyed
     * @param item the instance which has been destroyed
     */
    void itemDestroyed(ItemId id, ItemImpl item);
}
