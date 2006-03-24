/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.tck;

import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.TestCase;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.AccessDeniedException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.RepositoryHelper;

/**
 * The <code>Tester</code> starts the all tests and saves all tests.
 */
public class Tester {

    /** Object containing all tests */
    TestFinder tests;

    /** all test results are stored here */
    Map results;

    /** the test listener */
    TckTestRunner runner;

    /** the jsp writer */
    JspWriter writer;

    /** The string to be writen after finishing a suite */
    String finishedSuiteString;

    /** the exclude list */
    Map excludeList;

    /** the numberf of executed tests */
    int numOfTests;

    /**
     * The constructor...
     *
     * @param tests All tests found using <code>TestFinder</code>
     */
    public Tester(TestFinder tests, TckTestRunner runner, JspWriter writer, Map excludeList) {
        this.tests = tests;
        results = new HashMap();
        this.runner = runner;
        this.writer = writer;
        this.excludeList = excludeList;
        numOfTests = 0;

        // set the configuration for the to be tested repository
        AbstractJCRTest.helper = new RepositoryHelper(WebAppTestConfig.getCurrentConfig());
    }

    /**
     * Calling this method starts the testing. All test results will be stored in the
     * results hashmap for further use.
     *
     * @return the result list (map)
     */
    public Map run() {

        // get suites (level1, level2, ...)
        Iterator suites = tests.getSuites().keySet().iterator();

        while (suites.hasNext()) {
            TestSuite suite = (TestSuite) tests.getSuites().get(suites.next());

            TestResult result = new TestResult();
            result.addListener(runner);

            TestSuite updatedTS = applyExcludeList(suite);

            try {
                runner.resetNumberOfTests();
                updatedTS.run(result);
                results.putAll(runner.getResults());
                write(updatedTS);
                numOfTests += runner.getNumberOfTests();
            } catch (Exception e) {
                // ignore
            }
        }
        return results;
    }

    /**
     * This method goes through the exclude list and removes tests in the test suites
     * which should NOT be performed.
     *
     * @param suite Test suite
     * @return false if the whole test suite has to be skipped
     */
    private TestSuite applyExcludeList(TestSuite suite) {
        TestSuite updatedTS = new TestSuite();
        updatedTS.setName(suite.getName());
        Enumeration suiteMemberClasses = suite.tests();

        while (suiteMemberClasses.hasMoreElements()) {
            TestSuite testClass = (TestSuite) suiteMemberClasses.nextElement();
            String testClassName = testClass.toString();

            if (excludeList.containsKey(testClassName)) {
                continue;
            } else {
                TestSuite ts = new TestSuite();
                ts.setName(testClassName);
                List testcases = new ArrayList();
                boolean recreate = false;

                Enumeration testMethods = testClass.tests();
                while (testMethods.hasMoreElements()) {
                    TestCase tc = (TestCase) testMethods.nextElement();
                    String methodname = tc.getName();
                    if (excludeList.containsKey(testClassName + "#" + methodname)) {
                        recreate = true;
                    } else {
                        testcases.add(tc);
                    }
                }
                if (recreate) {
                    TestSuite recreatedTS = new TestSuite(ts.toString());
                    Iterator itr = testcases.iterator();

                    while (itr.hasNext()) {
                        recreatedTS.addTest((TestCase) itr.next());
                    }
                    updatedTS.addTest(recreatedTS);
                } else {
                    updatedTS.addTest(testClass);
                }

            }
        }
        return updatedTS;
    }


    public void setfinishedSuiteString(String line) {
        finishedSuiteString = line;
    }

    /**
     * Writes a predefined string to the output after finishing a test suite
     */
    private void write(TestSuite suite) throws IOException {
        if (writer != null) {
            writer.write(MessageFormat.format(finishedSuiteString, new String[]{suite.toString(), String.valueOf(suite.testCount())}));
        }
    }

    /**
     * This method stores the result underneath the passed node in this structure:
     * <pre>
     * node
     *  + suite name 1
     *    + test 1
     *    + test 2
     *    ...
     *  + suite name 2
     *    + test a
     *    + test b
     *    ...
     *  ....
     * </pre>
     * @param node parent <code>Node</code> for storage
     * @throws javax.jcr.RepositoryException
     * @throws ConstraintViolationException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.AccessDeniedException
     */
    public void storeResults(Node node) throws RepositoryException, ConstraintViolationException, InvalidItemStateException, AccessDeniedException {
        // create categories: level1, level2....
        Iterator keyItr = tests.getSuites().keySet().iterator();
        while (keyItr.hasNext()) {
            node.addNode((String) keyItr.next());
        }

        // save test results here
        Iterator itr = results.keySet().iterator();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            org.apache.jackrabbit.tck.TestResult tr = (org.apache.jackrabbit.tck.TestResult) results.get(key);
            String className = tr.getTest().getClass().getName();
            String testName = key.substring(0, key.indexOf("("));
            String keyword = (String) tests.getTests().get(className);
            if (keyword != null) {
                Node testResNode = node.addNode(keyword + "/" + key);
                testResNode.setProperty("name", testName);
                testResNode.setProperty("status", tr.getStatus());
                if (tr.getErrorMsg() != null) {
                   testResNode.setProperty("errrormsg", tr.getErrorMsg());
                }
                testResNode.setProperty("testtime", tr.getTestTime());
            }
        }
        node.save();
    }

    /**
     * Returns the number of executed tests
     *
     * @return number of executed tests
     */
    public int getNumberOfExecutedTests() {
        return numOfTests;
    }
}
