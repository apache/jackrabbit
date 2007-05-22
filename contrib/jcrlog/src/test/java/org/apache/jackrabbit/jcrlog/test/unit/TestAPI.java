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
package org.apache.jackrabbit.jcrlog.test.unit;

import org.apache.jackrabbit.jcrlog.RepositoryFactory;
import org.apache.jackrabbit.jcrlog.player.Player;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

/**
 * Simple unit test, checks the basic functionality only.
 *
 * @author Thomas Mueller
 *
 */
public class TestAPI extends TestCase {

    public static void main(String[] args) throws Exception {
        new TestAPI().test();
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public void test() throws Exception {
        stepCleanRepository();
        stepRunApp();
        stepCheckResults();
        stepCleanRepository();
        stepReplayLog();
        stepCheckResults();
    }

    private Session login(Repository repository) throws RepositoryException {
        return repository.login(new SimpleCredentials("test", "test"
                .toCharArray()));
    }

    private void stepCheckResults() throws RepositoryException {
        Repository repository = RepositoryFactory
                .open("apache/jackrabbit/transient");
        Session session = login(repository);
        Node root = session.getRootNode();
        Node test = root.getNode("test");
        assertNotNull(test);
        assertEquals("Hello", test.getProperty("name").getString());
        session.logout();
    }

    private void stepCleanRepository() throws RepositoryException {
        Repository repository = RepositoryFactory
                .open("apache/jackrabbit/transient");
        Session session = login(repository);
        Node root = session.getRootNode();
        if (root.hasNode("test")) {
            root.getNode("test").remove();
            session.save();
        }
        session.logout();
    }

    private void stepReplayLog() throws IOException {
        Player.execute("test.txt", false, true);
    }

    public void stepRunApp() throws Exception {
        Repository repository = RepositoryFactory
                .open("apache/jackrabbit/logger/file=test.txt;url=apache/jackrabbit/transient");
        // Repository repository =
        // RepositoryFactory.open("apache/jackrabbit/logger/file=test.txt;sysout=true;url=apache/jackrabbit/transient");
        Session session = login(repository);
        Node root = session.getRootNode();
        Node testNode = root.addNode("test");
        testNode.setProperty("name", "Hello");
        session.save();
        session.logout();
    }
}
