/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

public class MoveAtRootTest extends AbstractJCRTest {

    public void testMoveAtRoot() throws RepositoryException {
        final String pathA = "/" + getClass().getSimpleName() + "_A";
        final String pathB = "/" + getClass().getSimpleName() + "_B";

        final String testText = "Hello." + Math.random();

        Session s = getHelper().getSuperuserSession();
        if (s.itemExists(pathA)) {
            s.removeItem(pathA);
            s.save();
        }

        assertFalse(s.itemExists(pathA));

        Node n = s.getRootNode().addNode(pathA.substring(1));
        n.setProperty("text", testText);

        s.save();

        s.refresh(false);

        assertTrue(s.itemExists(pathA));

        // Move to pathB
        if (s.itemExists(pathB)) {
            s.removeItem(pathB);
            s.save();
        }

        assertFalse(s.itemExists(pathB));

        s.move(pathA, pathB);
        s.save();

        s.refresh(false);

        assertFalse(s.itemExists(pathA));
        assertTrue(s.itemExists(pathB));

        n = (Node) s.getItem(pathB);
        assertEquals(testText, n.getProperty("text").getString());
    }

}