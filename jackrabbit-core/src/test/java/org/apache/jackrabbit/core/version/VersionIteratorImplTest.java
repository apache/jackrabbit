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
package org.apache.jackrabbit.core.version;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

public class VersionIteratorImplTest extends TestCase {

    private static final int VERSION_COUNT = 10000;

    private final class DummyInternalVersion implements InternalVersion {

        private final InternalVersion[] successors;
        private NodeId id;

        public DummyInternalVersion(InternalVersion[] successors, NodeId id) {
            this.successors = successors;
            this.id = id;
        }

        public List<InternalVersion> getSuccessors() {
            return Arrays.asList(successors);
        }

        public NodeId getId() {
            return id;
        }

        public Calendar getCreated() {return null;}
        public InternalFrozenNode getFrozenNode() {return null;}
        public NodeId getFrozenNodeId() {return null;}
        public Name[] getLabels() {return null;}
        public Name getName() {return null;}
        public InternalVersion[] getPredecessors() {return null;}
        public InternalVersionHistory getVersionHistory() {return null;}
        public boolean hasLabel(Name label) {return false;}
        public boolean isMoreRecent(InternalVersion v) {return false;}
        public boolean isRootVersion() {return false;}
        public InternalVersionItem getParent() {return null;}
        public InternalVersion getLinearSuccessor(InternalVersion baseVersion) { return null; }
        public InternalVersion getLinearPredecessor() { return null; }
    }

    public void testVersionIterator() throws Exception {

        InternalVersion version = new DummyInternalVersion(new InternalVersion[] {}, NodeId.randomId());
        for (int i = 1; i < VERSION_COUNT; i++) {
            version = new DummyInternalVersion(new InternalVersion[] {version}, NodeId.randomId());
        }

        try {
            VersionIteratorImpl versionIteratorImpl = new VersionIteratorImpl(null, version);
            assertEquals(VERSION_COUNT, versionIteratorImpl.getSize());
        } catch (StackOverflowError e) {
            fail("Should be able to handle " + VERSION_COUNT + " versions.");
        }

    }

}
