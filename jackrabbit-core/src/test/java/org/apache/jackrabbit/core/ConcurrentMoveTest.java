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

import javax.jcr.InvalidItemStateException;

/**
 * <code>ConcurrentMoveTest</code>...
 */
public class ConcurrentMoveTest extends ConcurrentModificationBase {

    protected String srcAbsPath;

    protected String destAbsPath1;

    protected String destAbsPath2;

    protected void setUp() throws Exception {
        super.setUp();
        srcAbsPath = testRootNode.addNode("A").getPath();
        destAbsPath1 = testRootNode.addNode("B").getPath() + "/D";
        destAbsPath2 = testRootNode.addNode("C").getPath() + "/D";
        superuser.save();
    }

    public void testMove() throws Exception {
        superuser.move(srcAbsPath, destAbsPath1);
        session.move(srcAbsPath, destAbsPath2);
        superuser.save();
        try {
            session.save();
            fail("InvalidItemStateException expected");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }
}
