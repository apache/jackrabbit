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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RenameTest</code>...
 */
public class RenameTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(RenameTest.class);

    private String renamedName;
    private String renamePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        renamedName = "renamed";
        renamePath = srcParentNode.getPath() + "/" + renamedName;
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    public void testRename() throws RepositoryException {
        doMove(moveNode.getPath(), renamePath);
        assertEquals(moveNode.getName(), renamedName);
        superuser.save();
        assertEquals(moveNode.getName(), renamedName);
    }

    public void testRevertRename() throws RepositoryException {
        doMove(moveNode.getPath(), renamePath);
        assertEquals(moveNode.getName(), renamedName);

        superuser.refresh(false);
        assertEquals(moveNode.getName(), nodeName2);
    }
}