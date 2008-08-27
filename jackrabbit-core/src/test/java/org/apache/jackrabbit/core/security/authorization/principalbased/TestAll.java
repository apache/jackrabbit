/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.core.security.authorization.principalbased;

import junit.framework.TestCase;
import junit.framework.Test;
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

        suite.addTestSuite(ACLTemplateTest.class);
        suite.addTestSuite(EntryTest.class);
        suite.addTestSuite(GlobPatternTest.class);

        suite.addTestSuite(EvaluationTest.class);

        return suite;
    }
}
