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
package org.apache.jackrabbit.jcr.core.observation;

/**
 * The <code>EventDispatcher</code> interface defines a single method {@link
 * #dispatchEvents}, through this method a collection of {@link EventState}
 * instances is dispatched to registered {@link javax.jcr.observation.EventListener}s.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.2 $, $Date: 2004/08/02 16:19:47 $
 */
public interface EventDispatcher {

    /**
     * Dispatches the {@link EventStateCollection events} to all
     * registered {@link javax.jcr.observation.EventListener}s.
     *
     * @param events the {@link EventState}s to dispatch.
     */
    public void dispatchEvents(EventStateCollection events);
}
