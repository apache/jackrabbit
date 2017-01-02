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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

/**
 * <code>OnParentVersionInitializeTest</code> tests the {@link OnParentVersionAction#INITIALIZE INITIALIZE}
 * behaviour.
 *
 */
public class OnParentVersionInitializeTest extends AbstractOnParentVersionTest {

    protected void setUp() throws Exception {
        OPVAction = OnParentVersionAction.INITIALIZE;
        super.setUp();
    }

    /**
     * Test the restore of a OnParentVersion-INITIALIZE property
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreProp() throws RepositoryException {

        Node propParent = p.getParent();
        propParent.checkout();
        Version v = propParent.checkin();
        propParent.checkout();

        p.setValue(newPropValue);
        p.save();

        propParent.restore(v, false);

        assertEquals("On restore of a OnParentVersion-INITIALIZE property P, the current value of P must be left unchanged.", p.getString(), newPropValue);
    }

    /**
     * Test the restore of a OnParentVersion-INITIALIZE property
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestorePropJcr2() throws RepositoryException {

        Node propParent = p.getParent();
        VersionManager versionManager = propParent.getSession().getWorkspace().getVersionManager();
        String path = propParent.getPath();
        versionManager.checkout(path);
        Version v = versionManager.checkin(path);
        versionManager.checkout(path);

        p.setValue(newPropValue);
        p.getSession().save();

        versionManager.restore(v, false);

        assertEquals("On restore of a OnParentVersion-INITIALIZE property P, the current value of P must be left unchanged.", p.getString(), newPropValue);
    }
}
