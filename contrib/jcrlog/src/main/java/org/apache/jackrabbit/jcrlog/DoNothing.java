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
package org.apache.jackrabbit.jcrlog;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * This empty visitor / listener is used as a replacement for the visitor /
 * listener defined by the application.
 *
 * @author Thomas Mueller
 */
public class DoNothing implements ItemVisitor, EventListener {

    /**
     * Does nothing.
     *
     * @param node - ignored
     */
    public void visit(Property node) {
        // do nothing
    }

    /**
     * Does nothing.
     *
     * @param property - ignored
     */
    public void visit(Node property) {
        // do nothing
    }

    /**
     * Does nothing.
     *
     * @param events - ignored
     */
    public void onEvent(EventIterator events) {
        // do nothing
    }
}
