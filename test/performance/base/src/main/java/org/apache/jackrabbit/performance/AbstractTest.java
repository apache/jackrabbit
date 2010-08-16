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
package org.apache.jackrabbit.performance;

import javax.jcr.Credentials;
import javax.jcr.Repository;

/**
 * Abstract base class for individual performance benchmarks.
 */
public abstract class AbstractTest {

    private Repository repository;

    private Credentials credentials;

    public void beforeSuite() throws Exception {
    }

    public void beforeTest() throws Exception {
    }

    public abstract void runTest() throws Exception;

    public void afterTest() throws Exception {
    }

    public void afterSuite() throws Exception {
    }

    public Repository getRepository() {
        return repository;
    }

    void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public String toString() {
        String name = getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

}
