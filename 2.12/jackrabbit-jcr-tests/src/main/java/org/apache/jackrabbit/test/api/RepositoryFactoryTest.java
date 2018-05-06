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
package org.apache.jackrabbit.test.api;

import java.util.Collections;

import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.RepositoryStub;

/**
 * <code>RepositoryFactoryTest</code> checks if there is a repository factory
 * implementation and that is works according to the spec.
 */
public class RepositoryFactoryTest extends AbstractJCRTest {

    public void testDefaultRepository() throws Exception {
        // must not throw
        getRepositoryFactory().getRepository(null);
    }

    public void testEmptyParameters() throws Exception {
        // must not throw
        getRepositoryFactory().getRepository(Collections.EMPTY_MAP);
    }

    protected RepositoryFactory getRepositoryFactory()
            throws RepositoryException {
        String className = getProperty(RepositoryStub.REPOSITORY_FACTORY);
        if (className == null) {
            fail("Property '" + RepositoryStub.REPOSITORY_FACTORY + "' is not defined.");
        } else {
            try {
                return (RepositoryFactory) Class.forName(className).newInstance();
            } catch (Exception e) {
                fail(e.toString());
            }
        }
        return null;
    }
}
