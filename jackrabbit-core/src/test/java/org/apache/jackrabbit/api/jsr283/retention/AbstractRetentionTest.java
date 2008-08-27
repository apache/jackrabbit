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
package org.apache.jackrabbit.api.jsr283.retention;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractRetentionTest extends AbstractJCRTest {

    protected RetentionManager retentionMgr;

    protected void setUp() throws Exception {
        super.setUp();

        retentionMgr = getRetentionManager(superuser);
    }

    protected static RetentionManager getRetentionManager(Session s) throws RepositoryException, NotExecutableException {
        // TODO: fix (Replace by Session) test as soon as jackrabbit implements 283
        if (!(s instanceof SessionImpl)) {
            throw new NotExecutableException();
        }
        // TODO: uncomment again.
        // checkSupportedOption(Repository.OPTION_RETENTION_SUPPORTED);
        try {
            return ((SessionImpl) s).getRetentionManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }

    protected static void checkSupportedOption(Session s, String option) throws NotExecutableException {
        if (Boolean.FALSE.toString().equals(s.getRepository().getDescriptor(option))) {
            throw new NotExecutableException();
        }
    }
}
