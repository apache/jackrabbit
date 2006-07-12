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
package org.apache.jackrabbit.jcr2spi.state;

/**
 * <code>TransientItemStateListener</code> extends {@link ItemStateListener}
 * and adds to callbacks: {@link #stateOverlaid(ItemState)} and
 * {@link #stateUncovering(ItemState)}.
 */
public interface TransientItemStateListener extends ItemStateListener {

    /**
     * Called when an <code>ItemState</code> has been overlaid by some
     * other state that now takes its identity. This notification is sent
     * on the state being overlaid.
     *
     * @param overlayer the <code>ItemState</code> that overlays this state
     */
    public void stateOverlaid(ItemState overlayer);

    /**
     * Called when an <code>ItemState</code> is about to no longer overlay some
     * other item state. This notification is sent on the state overlaying
     * another state.
     *
     * @param overlayer the <code>ItemState</code> that overlaid another item
     *                  state. To get the overlaid state, invoke {@link
     *                  ItemState#getOverlayedState()}
     */
    public void stateUncovering(ItemState overlayer);

}
