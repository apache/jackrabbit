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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.jackrabbit.core.id.NodeId;

import junit.framework.TestCase;

public class VersionIteratorImplTest extends TestCase {

	public InternalVersion mockInternalVersion1(InternalVersion[] successors, NodeId id) {
		InternalVersion[] mockFieldVariableSuccessors;
		NodeId mockFieldVariableId;
		InternalVersion mockInstance = mock(InternalVersion.class);
		mockFieldVariableSuccessors = successors;
		mockFieldVariableId = id;
		when(mockInstance.getSuccessors()).thenAnswer((stubInvo) -> {
			return Arrays.asList(mockFieldVariableSuccessors);
		});
		when(mockInstance.getId()).thenAnswer((stubInvo) -> {
			return mockFieldVariableId;
		});
		return mockInstance;
	}

	private static final int VERSION_COUNT = 10000;

	public void testVersionIterator() throws Exception {

		// Construct mock object
		InternalVersion version = mockInternalVersion1(new InternalVersion[] {}, NodeId.randomId());
		for (int i = 1; i < VERSION_COUNT; i++) {
			// Construct mock object
			version = mockInternalVersion1(new InternalVersion[] { version }, NodeId.randomId());
		}

		try {
			VersionIteratorImpl versionIteratorImpl = new VersionIteratorImpl(null, version);
			assertEquals(VERSION_COUNT, versionIteratorImpl.getSize());
		} catch (StackOverflowError e) {
			fail("Should be able to handle " + VERSION_COUNT + " versions.");
		}

	}

}
