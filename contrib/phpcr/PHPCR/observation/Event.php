<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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


require_once 'PHPCR/RepositoryException.php';


/**
 * All events used by the ObservationManager system are subclassed from this interface
 * <p>
 * <b>Level 2 only</b>
 * <p>
 * For details see the <i>ObservationManager</i> section of the JCR standard document.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage observation
 */
interface Event
{
    /**
     * An event of this type is generated when a node is added.
     */
    const NODE_ADDED = 1;

    /**
     * An event of this type is generated when a node is removed.
     */
    const NODE_REMOVED = 2;

    /**
     * An event of this type is generated when a property is added.
     */
    const PROPERTY_ADDED = 4;

    /**
     * An event of this type is generated when a property is removed.
     */
    const PROPERTY_REMOVED = 8;

    /**
     * An event of this type is generated when a property is changed.
     */
    const PROPERTY_CHANGED = 16;


    /**
     * Returns the type of this event: a constant defined by this interface.
     * One of:
     * <ul>
     * <li><code>NODE_ADDED</code></li>
     * <li><code>NODE_REMOVED</code></li>
     * <li><code>PROPERTY_ADDED</code></li>
     * <li><code>PROPERTY_REMOVED</code></li>
     * <li><code>PROPERTY_CHANGED</code></li>
     * </ul>
     *
     * @return the type of this event.
     */
    public function getType();

    /**
     * Returns the absolute path of the parent node connected with this event.
     * The interpretation given to the returned path depends upon the type of the event:
     * <ul>
     *   <li>
     *     If the event type is <code>NODE_ADDED</code> then this method returns the absolute path of
     *     the node that was added.
     *   </li>
     *   <li>
     *     If the event type is <code>NODE_REMOVED</code> then this method returns the absolute path of
     *     the node that was removed.
     *   </li>
     *   <li>
     *     If the event type is <code>PROPERTY_ADDED</code> then this method returns the absolute path of
     *     the property that was added.
     *   </li>
     *   <li>
     *     If the event type is <code>PROPERTY_REMOVED</code> then this method returns the absolute path of
     *     the property that was removed.
     *   </li>
     *   <li>
     *     If the event type is <code>PROPERTY_CHANGED</code> then this method returns the absolute path of
     *     of the changed property.
     *   </li>
     * </ul>
     *
     * @throws RepositoryException if an error occurs.
     * @return the absolute path of the parent node connected with this event.
     */
    public function getPath();

    /**
     * Returns the user ID connected with this event. This is the string returned by getUserId of the session that
     * caused the event.
     *
     * @return a <code>String</code>.
     */
    public function getUserId();
}

?>