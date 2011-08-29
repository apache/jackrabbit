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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.Repository;
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

    private final int warmup = 10;

    private final int runtime = 50;

    private final Credentials credentials =
        new SimpleCredentials("admin", "admin".toCharArray());

    private Pattern repoPattern;
    private Pattern testPattern;

    protected void testPerformance(String name) throws Exception {
        repoPattern = Pattern.compile(System.getProperty("repo", "\\d\\.\\d"));
        testPattern = Pattern.compile(System.getProperty("only", ".*"));

        // Create a repository using the Jackrabbit default configuration
        testPerformance(name, getDefaultConfig());

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
                    testPerformance(
                            repositoryName, FileUtils.openInputStream(file));
                }
            }
        }
    }

    protected void testPerformance(String name, InputStream xml)
            throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            IOUtils.copy(xml, buffer);
        } finally {
            xml.close();
        }
        byte[] conf = buffer.toByteArray();

        runTest(new LoginTest(), name, conf);
        runTest(new LoginLogoutTest(), name, conf);
        runTest(new ReadPropertyTest(), name, conf);
        runTest(new SetPropertyTest(), name, conf);
        runTest(new SmallFileReadTest(), name, conf);
        runTest(new SmallFileWriteTest(), name, conf);
        runTest(new BigFileReadTest(), name, conf);
        runTest(new BigFileWriteTest(), name, conf);
        runTest(new ConcurrentReadTest(), name, conf);
        runTest(new ConcurrentReadWriteTest(), name, conf);
        runTest(new SimpleSearchTest(), name, conf);
        runTest(new SQL2SearchTest(), name, conf);
        runTest(new DescendantSearchTest(), name, conf);
        runTest(new SQL2DescendantSearchTest(), name, conf);
        runTest(new TwoWayJoinTest(), name, conf);
        runTest(new ThreeWayJoinTest(), name, conf);
        runTest(new CreateManyChildNodesTest(), name, conf);
        runTest(new UpdateManyChildNodesTest(), name, conf);
        runTest(new TransientManyChildNodesTest(), name, conf);
        runTest(new CreateUserTest(), name, conf);
        runTest(new PathBasedQueryTest(), name, conf);
        try {
            runTest(new AddGroupMembersTest(), name, conf);
            runTest(new GroupMemberLookupTest(), name, conf);
            runTest(new GroupGetMembersTest(), name, conf);
        } catch (NoClassDefFoundError e) {
            // ignore these tests if the required jackrabbit-api
            // extensions are not available
        }
    }

    private void runTest(AbstractTest test, String name, byte[] conf) {
        if (repoPattern.matcher(name).matches()
                &&  testPattern.matcher(test.toString()).matches()) {
            // Create the repository directory
            File dir = new File(
                    new File("target", "repository"),
                    name + "-" + test);
            dir.mkdirs();

            try {
                // Copy the configuration file into the repository directory
                File xml = new File(dir, "repository.xml");
                OutputStream output = FileUtils.openOutputStream(xml);
                try {
                    output.write(conf, 0, conf.length);
                } finally {
                    output.close();
                }

                // Create the repository
                RepositoryImpl repository = createRepository(dir, xml);
                try {
                    // Run the test
                    DescriptiveStatistics statistics = runTest(test, repository);
                    if (statistics.getN() > 0) {
                        writeReport(test.toString(), name, statistics);
                    }
                } finally {
                    repository.shutdown();
                }
            } catch (Throwable t) {
                System.out.println(
                        "Unable to run " + test + ": " + t.getMessage());
            } finally {
                FileUtils.deleteQuietly(dir);
            }
        }
    }

    private DescriptiveStatistics runTest(
            AbstractTest test, Repository repository)
            throws Exception {
        DescriptiveStatistics statistics = new DescriptiveStatistics();

        test.setUp(repository, credentials);
        try {
            // Run a few iterations to warm up the system
            long warmupEnd = System.currentTimeMillis() + warmup * 1000;
            while (System.currentTimeMillis() < warmupEnd) {
                test.execute();
            }

            // Run test iterations, and capture the execution times
            long runtimeEnd = System.currentTimeMillis() + runtime * 1000;
            while (System.currentTimeMillis() < runtimeEnd) {
                statistics.addValue(test.execute());
            }
        } finally {
            test.tearDown();
        }

        return statistics;
    }

    private void writeReport(
            String test, String name, DescriptiveStatistics statistics)
            throws IOException {
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
    }

    protected InputStream getDefaultConfig() {
        return RepositoryImpl.class.getResourceAsStream("repository.xml");
    }

    protected RepositoryImpl createRepository(File dir, File xml)
            throws RepositoryException, ConfigurationException {
        return RepositoryImpl.create(
                RepositoryConfig.create(xml.getPath(), dir.getPath()));
    }

}
