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

/**
 * The <code>SessionListener</code> interface allows an implementing
 * object to be informed about changes on a <code>Session</code>.
 *
 * @see SessionImpl#addListener
 */
public interface SessionListener {

    /**
     * Called when a <code>Session</code> is about to be 'closed' by
     * calling <code>{@link javax.jcr.Session#logout()}</code>. At this
     * moment the session is still valid.
     *
     * @param session the <code>Session</code> that is about to be 'closed'
     */
    void loggingOut(SessionImpl session);

    /**
     * Called when a <code>Session</code> has been 'closed' by
     * calling <code>{@link javax.jcr.Session#logout()}</code>.
     *
     * @param session the <code>Session</code> that has been 'closed'
     */
    void loggedOut(SessionImpl session);
}
