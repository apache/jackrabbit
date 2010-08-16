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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class PerformanceTestSuite {

    private final Repository repository;

    private final Credentials credentials;

    private final int warmup = 10;

    private final int runtime = 50;

    public PerformanceTestSuite(
            Repository repository, Credentials credentials) {
        this.repository = repository;
        this.credentials = credentials;
    }

    public DescriptiveStatistics runTest(AbstractTest test) throws Exception {
        DescriptiveStatistics statistics = new DescriptiveStatistics();

        test.setRepository(repository);
        test.setCredentials(credentials);

        test.beforeSuite();

        // Run a few iterations to warm up the system
        long warmupEnd = System.currentTimeMillis() + warmup * 1000;
        while (System.currentTimeMillis() < warmupEnd) {
            test.beforeTest();
            test.runTest();
            test.afterTest();
        }

        // Run test iterations, and capture the execution times
        long runtimeEnd = System.currentTimeMillis() + runtime * 1000;
        while (System.currentTimeMillis() < runtimeEnd) {
            test.beforeTest();
            long start = System.currentTimeMillis();
            test.runTest();
            statistics.addValue(System.currentTimeMillis() - start);
            test.afterTest();
        }

        test.afterSuite();

        return statistics;
    }

    public void run(AbstractTest test) throws Exception {
        DescriptiveStatistics statistics = runTest(test);
        System.out.format(
                "%-36.36s  %6.0f  %6.0f  %6.0f  %6.0f  %6d%n",
                test,
                statistics.getMean(),
                statistics.getStandardDeviation(),
                statistics.getMin(),
                statistics.getMax(),
                statistics.getN());
    }

    public static void main(String[] args) throws Exception {
        File file = new File("target", "repository");
        RepositoryImpl repository =
            RepositoryImpl.create(RepositoryConfig.install(file));
        Credentials credentials =
            new SimpleCredentials("admin", "admin".toCharArray());

        System.out.println(
                "Test case                           "
                + "    Mean    Sdev     Min     Max   Count");
        System.out.println(
                "------------------------------------"
                + "----------------------------------------");
        PerformanceTestSuite suite =
            new PerformanceTestSuite(repository, credentials);
        suite.runTest(new LoginTest());
        suite.runTest(new LoginLogoutTest());
        // suite.runTest(new RefreshTest());
        // suite.runTest(new SmallFileReadTest());
        // suite.runTest(new SmallFileWriteTest());
        // suite.runTest(new BigFileReadTest());
        // suite.runTest(new BigFileWriteTest());

        System.out.println();
        System.out.println("Test environment");
        System.out.format(
                "  JCR: %s version %s by %s%n",
                repository.getDescriptor(Repository.REP_NAME_DESC),
                repository.getDescriptor(Repository.REP_VERSION_DESC),
                repository.getDescriptor(Repository.REP_VENDOR_DESC));
        System.out.format(
                "  JRE: %s by %s%n",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
        System.out.format(
                "  JVM: %s version %s by %s%n",
                System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version"),
                System.getProperty("java.vm.vendor"));
        System.out.format(
                "   OS: %s version %s for %s%n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));

        repository.shutdown();
        FileUtils.deleteDirectory(file);
    }

}
