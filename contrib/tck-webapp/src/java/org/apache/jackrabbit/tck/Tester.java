/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.*;
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

    /**
     * The constructor...
     *
     * @param tests All tests found using <code>TestFinder</code>
     */
    public Tester(TestFinder tests, TckTestRunner runner) {
        this.tests = tests;
        results = new HashMap();
        this.runner = runner;

        // set the configuration for the to be tested repository
        AbstractJCRTest.helper = new RepositoryHelper(WebAppTestConfig.getConfig());
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
            try {
                suite.run(result);
                results.putAll(runner.getResults());
            } catch (Exception e) {
                // ignore
            }
        }
        return results;
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
     * @throws RepositoryException
     * @throws ConstraintViolationException
     * @throws InvalidItemStateException
     * @throws AccessDeniedException
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
}
