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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.cluster.Update;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;

/**
 * Package-private utility class used as a sentinel by the
 * {@link SharedItemStateManager} class.
 */
public class DummyUpdateEventChannel implements UpdateEventChannel {

    public void updatePrepared(Update update) {}

    public void updateCreated(Update update) {}

    public void updateCommitted(Update update, String path) {}

    public void updateCancelled(Update update) {}

    public void setListener(UpdateEventListener listener) {}

}