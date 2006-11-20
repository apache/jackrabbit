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
package org.apache.jackrabbit.jcr2spi.state.entry;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;

import java.lang.ref.WeakReference;

/**
 * <code>ChildItemReference</code> implements base functionality for child node
 * and property references.
 * @see ChildNodeReference
 * @see PropertyReference
 */
abstract class ChildItemReference implements ChildItemEntry {

    /**
     * Cached weak reference to the target NodeState.
     */
    private WeakReference target;

    /**
     * The name of the target item state.
     */
    private final QName name;

    /**
     * The parent that owns this <code>ChildItemReference</code>.
     */
    protected final NodeState parent;

    /**
     * The item state factory to create the the item state.
     */
    protected final ItemStateFactory isf;

    /**
     * Creates a new <code>ChildItemReference</code> with the given parent
     * <code>NodeState</code>.
     *
     * @param parent the <code>NodeState</code> that owns this child node
     *               reference.
     * @param name   the name of the child item.
     * @param isf    the item state factory to create the item state.
     */
    public ChildItemReference(NodeState parent, QName name, ItemStateFactory isf) {
        this.parent = parent;
        this.name = name;
        this.isf = isf;
    }

    /**
     * Creates a new <code>ChildItemReference</code> with the given parent
     * <code>NodeState</code> and an already initialized child item state.
     *
     * @param parent the <code>NodeState</code> that owns this child node
     *               reference.
     * @param child  the child item state.
     * @param name   the name of the child item.
     * @param isf    the item state factory to re-create the item state.
     */
    public ChildItemReference(NodeState parent, ItemState child, QName name, ItemStateFactory isf) {
        this.parent = parent;
        this.name = name;
        this.isf = isf;
        this.target = new WeakReference(child);
    }
    
    /**
     * Resolves this <code>ChildItemReference</code> and returns the target
     * <code>ItemState</code> of this reference. This method may return a
     * cached <code>ItemState</code> if this method was called before already
     * otherwise this method will forward the call to {@link #doResolve()}
     * and cache its return value.
     *
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    protected ItemState resolve()
            throws NoSuchItemStateException, ItemStateException {
        // check if cached
        if (target != null) {
            ItemState state = (ItemState) target.get();
            if (state != null) {
                return state;
            }
        }
        // not cached. retrieve and keep weak reference to state
        ItemState state = doResolve();
        target = new WeakReference(state);
        return state;
    }

    /**
     * @return <code>true</code> if this reference is resolved;
     *         <code>false</code> otherwise.
     */
    protected boolean isResolved() {
        ItemState state = null;
        if (target != null) {
            state = (ItemState) target.get();
        }
        return state != null;
    }

    /**
     * Resolves this <code>ChildItemReference</code> and returns the target
     * <code>ItemState</code> of this reference.
     *
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    protected abstract ItemState doResolve()
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @inheritDoc
     * @see ChildItemEntry#getName()
     */
    public QName getName() {
        return name;
    }

    /**
     * @inheritDoc
     * @see ChildItemEntry#isAvailable()
     */
    public boolean isAvailable() {
        return isResolved();
    }
}
