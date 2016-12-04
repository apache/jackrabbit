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

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import junit.framework.TestCase;

/**
 * Test case for the {@link TransientRepository} class. Currently only the
 * static repository descriptor access is tested due to the difficulty of
 * setting up mock {@link RepositoryImpl} instances.
 */
public class TransientRepositoryTest extends TestCase {

    /**
     * The TransientRepository instance under test.
     */
    private Repository repository;

    /**
     * Creates the TransientRepository instance used in testing.
     */
    protected void setUp() throws IOException {
        repository = new TransientRepository(new TransientRepository.RepositoryFactory() {
                public RepositoryImpl getRepository() throws RepositoryException {
                    throw new UnsupportedRepositoryOperationException();
                }
            }, null);
    }

    /**
     * Tests that {@link TransientRepository} returns descriptor keys
     * even before the underlying repository has been initialized.
     */
    public void testGetDescriptorKeys() {
        String[] keys = repository.getDescriptorKeys();
        assertNotNull(keys);
        assertTrue(keys.length > 0);
    }

    /**
     * Tests that {@link TransientRepository} returns descriptor values
     * even before the underlying repository has been initialized.
     */
    public void testGetDescriptor() {
        String expected =
            "Content Repository API for Java(TM) Technology Specification";
        String actual = repository.getDescriptor(Repository.SPEC_NAME_DESC);
        assertEquals(expected, actual);
    }

}
