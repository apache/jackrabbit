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
package org.apache.jackrabbit.core.integration.benchmark;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * A simple benchmark application for Jackrabbit.
 */
public class SimpleBench {

    int run;
    long start;
    Repository repository;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; i++) {
            new SimpleBench().test(i);
        }
    }

    void start() {
        start = System.currentTimeMillis();
    }

    void end(String message) {
        long time = System.currentTimeMillis() - start;
        if (run > 0) {
            System.out.println("run: " + run + "; time: " + time + " ms; task: " + message);
        }
    }

    void test(int run) throws Exception {
        this.run = run;
        new File("target/jcr.log").delete();
        FileUtils.deleteQuietly(new File("repository"));

        start();
        repository = new TransientRepository();
        Session session = repository.login(new SimpleCredentials("", "".toCharArray()));
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        session.getRootNode().addNode("test");
        session.save();
        end("init");
        Node node = session.getRootNode().getNode("test");
        Node n = null;
        int len = run == 0 ? 100 : 1000;
        start();
        for (int i = 0; i < len; i++) {
            if (i % 100 == 0) {
                n = node.addNode("sub" + i);
            }
            Node x = n.addNode("x" + (i % 100));
            x.setProperty("name", "John");
            x.setProperty("firstName", "Doe");
            session.save();
        }
        end("addNodes");
        session.logout();
    }

}
