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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import javax.jcr.SimpleCredentials;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractPerformanceTest {

    protected void testPerformance(String name) throws Exception {
        Set<String> tests = new HashSet<String>();
        Set<String> names = new HashSet<String>();
        String selected = System.getProperty("only");
        if (selected != null && selected.length() > 0) {
            int colon = selected.indexOf(':');
            if (colon != -1) {
                names.addAll(Arrays.asList(selected.substring(colon + 1).split(",")));
                selected = selected.substring(0, colon);
            }
            tests.addAll(Arrays.asList(selected.split(",")));
        }

        // Create a repository using the Jackrabbit default configuration
        if (names.isEmpty() || names.contains(name)) {
            testPerformance(
                    name,
                    RepositoryImpl.class.getResourceAsStream("repository.xml"),
                    tests);
        }

        // Create repositories for any special configurations included
        File directory = new File(new File("src", "test"), "resources");
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                String xml = file.getName();
                if (file.isFile() && xml.endsWith(".xml")) {
                    String repositoryName =
                        name + "-" + xml.substring(0, xml.length() - 4);
                    if (names.isEmpty() || names.contains(repositoryName)) {
                        testPerformance(
                                repositoryName,
                                FileUtils.openInputStream(file),
                                tests);
                    }
                }
            }
        }
    }

    private void testPerformance(
            String name, InputStream xml, Set<String> tests) throws Exception {
        RepositoryImpl repository = createRepository(name, xml);
        try {
            testPerformance(name, repository, tests);
        } finally {
            repository.shutdown();
        }
    }

    private void testPerformance(
            String name, RepositoryImpl repository, Set<String> tests) {
        PerformanceTestSuite suite = new PerformanceTestSuite(
                repository,
                new SimpleCredentials("admin", "admin".toCharArray()));
        runTest(suite, new LoginTest(), name, tests);
        runTest(suite, new LoginLogoutTest(), name, tests);
        runTest(suite, new SmallFileReadTest(), name, tests);
        runTest(suite, new SmallFileWriteTest(), name, tests);
        runTest(suite, new BigFileReadTest(), name, tests);
        runTest(suite, new BigFileWriteTest(), name, tests);
        runTest(suite, new ConcurrentReadTest(), name, tests);
        runTest(suite, new ConcurrentReadWriteTest(), name, tests);
        runTest(suite, new SimpleSearchTest(), name, tests);
        runTest(suite, new TwoWayJoinTest(), name, tests);
        runTest(suite, new ThreeWayJoinTest(), name, tests);
        runTest(suite, new CreateManyChildNodesTest(), name, tests);
        runTest(suite, new UpdateManyChildNodesTest(), name, tests);
        runTest(suite, new TransientManyChildNodesTest(), name, tests);
        runTest(suite, new CreateUserTest(), name, tests);
        runTest(suite, new AddGroupMembersTest(), name, tests);
        runTest(suite, new GroupMemberLookupTest(), name, tests);
        runTest(suite, new GroupGetMembersTest(), name, tests);
    }

    private void runTest(
            PerformanceTestSuite suite, AbstractTest test, String name,
            Set<String> tests) {
        if (!tests.isEmpty() && !tests.contains(test.toString())) {
            return;
        }

        try {
            DescriptiveStatistics statistics = suite.runTest(test);

            File base = new File("..", "base");
            File target = new File(base, "target");
            File report = new File(target, test + ".txt");
            boolean needsPrefix = !report.exists();

            PrintWriter writer = new PrintWriter(
                    new FileWriterWithEncoding(report, "UTF-8", true));
            try {
                if (needsPrefix) {
                    writer.format(
                            "# %-34.34s     avg     std     min     max   count%n",
                            test);
                }

                writer.format(
                        "%-36.36s  %6.0f  %6.0f  %6.0f  %6.0f  %6d%n",
                        name,
                        statistics.getMean(),
                        statistics.getStandardDeviation(),
                        statistics.getMin(),
                        statistics.getMax(),
                        statistics.getN());
            } finally {
                writer.close();
            }
        } catch (Throwable t) {
            System.out.println("Unable to run " + test + ": " + t.getMessage());
        }
    }

    /**
     * Creates a named test repository with the given configuration file.
     *
     * @param name name of the repository
     * @param xml input stream for reading the repository configuration
     * @throws Exception if the repository could not be created
     */
    private RepositoryImpl createRepository(String name, InputStream xml)
            throws Exception {
        File directory = new File(new File("target", "repository"), name);
        File configuration = new File(directory, "repository.xml");

        // Copy the configuration file into the repository directory
        try {
            OutputStream output = FileUtils.openOutputStream(configuration);
            try {
                IOUtils.copy(xml, output);
            } finally {
                output.close();
            }
        } finally {
            xml.close();
        }

        // Create the repository
        return RepositoryImpl.create(RepositoryConfig.create(
                configuration.getPath(), directory.getPath()));
    }

}
