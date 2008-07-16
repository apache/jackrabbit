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
package org.apache.jackrabbit.api.observation;

import javax.jcr.observation.EventListener;

/**
 * This is a marker interface that can be used by client code
 * to inform the repository that this listener should only be
 * invoked for local changes.
 * Local changes are changes happening on the same node in a 
 * clustered environment.
 * If there is no clustered environment, changes are always
 * happening on the "current node" and therefore this listener
 * is treated like a usual event listener.
 * 
 * Please note: if the jcr implementation does not value this
 *              interface, the listener is still invoked for
 *              all changes happening in the repository.
 *              Therefore it is advisable to check the repository
 *              implementation on startup if this feature is
 *              required! The {@link javax.jcr.Repository#getDescriptor(String)}
 *              method with {@link #OPTION_LOCAL_EVENT_LISTENER} can
 *              be used to test the implementation.
 */
public interface LocalEventListener extends EventListener {

    /**
     * The presence of this key indicates that this implementation
     * supports local event listeners.
     */
     String OPTION_LOCAL_EVENT_LISTENER = "option.observation.localeventlistener";
    
}
