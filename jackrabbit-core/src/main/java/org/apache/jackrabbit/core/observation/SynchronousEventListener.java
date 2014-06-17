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
package org.apache.jackrabbit.core.observation;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * Defines a marker interface for {@link javax.jcr.observation.EventListener}
 * implementations that wish a synchronous notification of changes to the
 * workspace. That is, a <code>SynchronousEventListener</code> is called before
 * the call to {@link javax.jcr.Item#save()} returns. In contrast, a regular
 * {@link javax.jcr.observation.EventListener} might be called after
 * <code>save()</code> returns.
 * <p>
 * <b>Important note</b>: an implementation of {@link SynchronousEventListener}
 * <b>must not</b> modify content with the thread that calls {@link
 * #onEvent(EventIterator)} otherwise inconsistencies may occur.
 */
public interface SynchronousEventListener extends EventListener {
}
