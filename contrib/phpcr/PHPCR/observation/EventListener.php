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


require_once 'PHPCR/observation/EventIterator.php';


/**
 * An event listener.
 * <p>
 * <b>Level 2 only</b>
 * <p>
 * An <code>EventListener</code> can be registered via the
 * <code>observation.ObservationManager</code> object. Event listeners are
 * notified asynchronously, and see events after they occur and the transaction
 * is committed. An event listener only sees events for which the ticket that
 * registered it has sufficient access rights.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage observation
 */
interface EventListener
{
    /**
     * Gets called when an event occurs.
     *
     * @param events The event set recieved.
     */
    public function onEvent( EventIterator $events );
}

?>