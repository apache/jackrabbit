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
package org.apache.jackrabbit.api.jsr283.observation;

import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * <code>Event</code> is a preliminary interface that contains the new methods
 * introduce in JSR 283.
 * <p/>
 * <b>This interface will be removed once JSR 283 is final.</b>
 */
public interface Event extends javax.jcr.observation.Event {

    /**
     * Generated on persist when a node is moved.
     * <ul>
     * <li>{@link #getPath} returns the absolute path of the destination of the move.</li>
     * <li>{@link #getIdentifier} returns the identifier of the moved node.
     * <li>
     *   {@link #getInfo} If the method that caused this event was
     *   a {@link javax.jcr.Session#move Session.move} or {@link javax.jcr.Workspace#move Workspace.move}
     *   then the returned {@link java.util.Map Map} has keys <code>srcAbsPath</code> and <code>destAbsPath</code>
     *   with values corresponding to the parameters passed to the <code>move</code> method.
     *   <p>
     *   If the method that caused this event was a {@link javax.jcr.Node#orderBefore Node.orderBefore}
     *   then the returned <code>Map</code> has keys <code>srcChildRelPath</code> and <code>destChildRelPath</code>
     *   with values corresponding to the parameters passed to the <code>orderBefore</code> method.
     * </li>
     * </ul>
     *
     * @since JCR 2.0
     */
    public static final int NODE_MOVED = 0x20;

    /**
     * If event bundling is supported, this event is used to indicate a
     * bundle boundary within the event journal.
     * <ul>
     * <li>{@link #getPath} returns <code>null</code>.</li>
     * <li>{@link #getIdentifier} returns <code>null</code>.</li>
     * <li>{@link #getInfo} returns an empty <code>Map</code> object.</li>
     * </ul>
     *
     * @since JCR 2.0
     */
    public static final int PERSIST = 0x40;

    /**
     * Returns the identifier associated with this event or <code>null</code>
     * if this event has no associated identifier. The meaning of the associated
     * identifier depends upon the type of the event.
     * See event type constants above.
     *
     * @return the identifier associated with this event or <code>null</code>.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public String getIdentifier() throws RepositoryException;

    /**
     * Returns the information map associated with this event.
     * The meaning of the map depends upon the type of the event.
     * See event type constants above.
     *
     * @return A <code>Map</code> containing parameter information
     * for instances of a <code>NODE_MOVED</code> event.
     *
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public Map getInfo() throws RepositoryException;

    /**
     * Returns the user data set through <code>ObservationManager.setUserData()</code>
     * on the <code>ObservationManager</code> bound to the <code>Session</code> that caused
     * the event.
     *
     * @return String
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public String getUserData() throws RepositoryException;

    /**
     * Returns the date when the change was persisted that caused this event.
     * The date is represented as a millisecond value that is an offset from the
     * Epoch, January 1, 1970 00:00:00.000 GMT (Gregorian). The granularity of
     * the returned value is implementation dependent.
     *
     * @return the date when the change was persisted that caused this event.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public long getDate() throws RepositoryException;
}
