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
package org.apache.jackrabbit.core.retention;

import javax.jcr.RepositoryException;
import javax.jcr.retention.RetentionPolicy;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractRetentionTest extends org.apache.jackrabbit.test.api.retention.AbstractRetentionTest {

    @Override
    protected RetentionPolicy getApplicableRetentionPolicy() throws NotExecutableException, RepositoryException {
        return getApplicableRetentionPolicy("retentionPolicyName");
    }

    protected RetentionPolicy getApplicableRetentionPolicy(String jcrName) throws NotExecutableException, RepositoryException {
        // TODO: move to repositoryStub/helper and adjust accordingly
        return RetentionPolicyImpl.createRetentionPolicy(jcrName, superuser);
    }
}