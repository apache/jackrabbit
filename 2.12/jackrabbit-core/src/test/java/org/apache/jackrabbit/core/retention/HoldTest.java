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
package org.apache.jackrabbit.core.retention;

import javax.jcr.RepositoryException;
import javax.jcr.retention.Hold;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation specific hold tests.
 */
public class HoldTest extends AbstractRetentionTest {

    private static Logger log = LoggerFactory.getLogger(HoldTest.class);

    @Override
    protected void tearDown() throws Exception {
        try {
            superuser.refresh(false);
            Hold[] holds = retentionMgr.getHolds(testNodePath);
            for (Hold hold : holds) {
                retentionMgr.removeHold(testNodePath, hold);
            }
            superuser.save();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        super.tearDown();
    }

    public void testAddHoldTwice() throws RepositoryException, NotExecutableException {
        Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);

        try {
            retentionMgr.addHold(testNodePath, getHoldName(), true);
            fail("cannot add the same hold twice");
        } catch (RepositoryException e) {
            // success
        }

        superuser.save();
        try {
            retentionMgr.addHold(testNodePath, getHoldName(), true);
            fail("cannot add the same hold twice");
        } catch (RepositoryException e) {
            // success
        }        
    }
    
    public void testRemoveInvalidHold() throws RepositoryException, NotExecutableException {
        final Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);

        try {
            Hold invalidH = new Hold() {
                public boolean isDeep() throws RepositoryException {
                    return h.isDeep();
                }
                public String getName() throws RepositoryException {
                    return h.getName();
                }
            };
            retentionMgr.removeHold(testNodePath, invalidH);
            fail("An invalid hold impl. should not be removable.");

        } catch (RepositoryException e) {
            // success
        }
    }

    public void testRemoveInvalidHold2() throws RepositoryException, NotExecutableException {
        final Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);

        try {
            Hold invalidH = new Hold() {
                public boolean isDeep() throws RepositoryException {
                    return h.isDeep();
                }
                public String getName() throws RepositoryException {
                    return "anyName";
                }
            };
            retentionMgr.removeHold(testNodePath, invalidH);
            fail("An invalid hold impl. should not be removable.");

        } catch (RepositoryException e) {
            // success
        }
    }

    public void testRemoveInvalidHold3() throws RepositoryException, NotExecutableException {
        final Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);

        try {
            Hold invalidH = new Hold() {
                public boolean isDeep() throws RepositoryException {
                    return !h.isDeep();
                }
                public String getName() throws RepositoryException {
                    return h.getName();
                }
            };
            retentionMgr.removeHold(testNodePath, invalidH);
            fail("An invalid hold impl. should not be removable.");

        } catch (RepositoryException e) {
            // success
        }
    }
}