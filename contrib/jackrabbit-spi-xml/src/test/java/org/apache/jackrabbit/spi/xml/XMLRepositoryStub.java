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
package org.apache.jackrabbit.spi.xml;

import java.io.InputStream;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;

/**
 * XML repository stub.
 */
public class XMLRepositoryStub extends RepositoryStub {

    /**
     * The test repository
     */
    private Repository repository;

    /**
     * Constructor as required by the JCR TCK.
     *
     * @param env environment properties.
     */
    public XMLRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * Returns the test repository.
     *
     * @return the test repository
     * @throws RepositoryStubException if the test repository can not be started
     */
    public synchronized Repository getRepository()
            throws RepositoryStubException {
        if (repository == null) {
            try {
                InputStream xml =
                    XMLRepositoryStub.class.getResourceAsStream("repository.xml");
                repository = new XMLRepositoryFactory().getRepository(xml);
            } catch (Exception e) {
                throw new RepositoryStubException(e.getMessage());
            }
        }
        return repository;
    }

}
