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
package org.apache.jackrabbit.core.security.simple;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.junit.Test;

public class SimpleSecurityManagerTest extends TestCase {

    private Repository repository;

    @Override
    public void setUp() throws RepositoryException {
        String file = "src/test/resources/org/apache/jackrabbit/core/security/simple/simple_repository.xml";
        RepositoryConfig config = RepositoryConfig.create(file, "target/simple_repository");
        repository = RepositoryImpl.create(config);
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3697">JCR-3697</a>
     */
    @Test
    public void testRemove() throws RepositoryException {
        Session s = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Node n = s.getRootNode().addNode(("a"));
        s.save();

        n.remove();
        s.save();
    }
}