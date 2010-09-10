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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public abstract class AbstractPerformanceTest {

    protected void testPerformance(String name) throws Exception {
        String only = System.getProperty("only", ".*:.*");
        int colon = only.indexOf(':');
        if (colon == -1) {
            colon = only.length();
            only = only + ":-1";
        }

        Pattern testPattern = Pattern.compile(only.substring(0, colon));
        Pattern namePattern = Pattern.compile(only.substring(colon + 1));

        // Create a repository using the Jackrabbit default configuration
        if (namePattern.matcher(name).matches()) {
            testPerformance(name, getDefaultConfig(), testPattern);
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
                    if (namePattern.matcher(repositoryName).matches()) {
                        testPerformance(
                                repositoryName,
                                FileUtils.openInputStream(file),
                                testPattern);
                    }
                }
            }
        }
    }

    private void testPerformance(
            String name, InputStream xml, Pattern testPattern)
            throws Exception {
        RepositoryImpl repository = createRepository(name, xml);
        try {
            testPerformance(name, repository, testPattern);
        } finally {
            repository.shutdown();
        }
    }

    private void testPerformance(
            String name, RepositoryImpl repository, Pattern testPattern) {
        PerformanceTestSuite suite = new PerformanceTestSuite(
                repository,
                new SimpleCredentials("admin", "admin".toCharArray()));
        runTest(suite, new LoginTest(), name, testPattern);
        runTest(suite, new LoginLogoutTest(), name, testPattern);
        runTest(suite, new SetPropertyTest(), name, testPattern);
        runTest(suite, new SmallFileReadTest(), name, testPattern);
        runTest(suite, new SmallFileWriteTest(), name, testPattern);
        runTest(suite, new BigFileReadTest(), name, testPattern);
        runTest(suite, new BigFileWriteTest(), name, testPattern);
        runTest(suite, new ConcurrentReadTest(), name, testPattern);
        runTest(suite, new ConcurrentReadWriteTest(), name, testPattern);
        runTest(suite, new SimpleSearchTest(), name, testPattern);
        runTest(suite, new TwoWayJoinTest(), name, testPattern);
        runTest(suite, new ThreeWayJoinTest(), name, testPattern);
        runTest(suite, new CreateManyChildNodesTest(), name, testPattern);
        runTest(suite, new UpdateManyChildNodesTest(), name, testPattern);
        runTest(suite, new TransientManyChildNodesTest(), name, testPattern);
        runTest(suite, new CreateUserTest(), name, testPattern);
        try {
            runTest(suite, new AddGroupMembersTest(), name, testPattern);
            runTest(suite, new GroupMemberLookupTest(), name, testPattern);
            runTest(suite, new GroupGetMembersTest(), name, testPattern);
        } catch (NoClassDefFoundError e) {
            // ignore these tests if the required jackrabbit-api
            // extensions are not available
        }
    }

    private void runTest(
            PerformanceTestSuite suite, AbstractTest test, String name,
            Pattern testPattern) {
        if (!testPattern.matcher(test.toString()).matches()) {
            return;
        }

        try {
            DescriptiveStatistics statistics = suite.runTest(test);

            File report = new File("target", test + ".txt");
            boolean needsPrefix = !report.exists();

            PrintWriter writer = new PrintWriter(
                    new FileWriterWithEncoding(report, "UTF-8", true));
            try {
                if (needsPrefix) {
                    writer.format(
                            "# %-34.34s     min     10%%     50%%     90%%     max%n",
                            test);
                }

                writer.format(
                        "%-36.36s  %6.0f  %6.0f  %6.0f  %6.0f  %6.0f%n",
                        name,
                        statistics.getMin(),
                        statistics.getPercentile(10.0),
                        statistics.getPercentile(50.0),
                        statistics.getPercentile(90.0),
                        statistics.getMax());
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
        return createRepository(directory, configuration);
    }

    protected InputStream getDefaultConfig() {
        return RepositoryImpl.class.getResourceAsStream("repository.xml");
    }

    protected RepositoryImpl createRepository(
            File directory, File configuration)
            throws RepositoryException, ConfigurationException {
        return RepositoryImpl.create(RepositoryConfig.create(
                configuration.getPath(), directory.getPath()));
    }

}
