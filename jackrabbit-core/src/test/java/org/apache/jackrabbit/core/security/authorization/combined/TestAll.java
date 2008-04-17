package org.apache.jackrabbit.core.security.authorization.combined;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite
 */
public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("security.authorization.combined tests");

        suite.addTestSuite(PolicyTemplateImplTest.class);
        suite.addTestSuite(PolicyEntryImplTest.class);
        suite.addTestSuite(GlobPatternTest.class);

        suite.addTestSuite(EvaluationTest.class);

        return suite;
    }
}
