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
package org.apache.jackrabbit.jcr2spi.version;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.jackrabbit.test.api.version.VersionLabelTest;

public class LabelTest extends VersionLabelTest {

    public void testRemovedLabel2() throws RepositoryException {
        vHistory.addVersionLabel(version.getName(), versionLabel, false);
        vHistory.removeVersionLabel(versionLabel);

        List<String> labels = Arrays.asList(vHistory.getVersionLabels());
        assertFalse("VersionHistory.getVersionLabels() must not return a removed label.",labels.contains(versionLabel));
    }

    public void testRemovedLabel3() throws RepositoryException {
        vHistory.addVersionLabel(version.getName(), versionLabel, false);
        vHistory.removeVersionLabel(versionLabel);

        List<String> labels = Arrays.asList(vHistory.getVersionLabels(version));
        assertFalse("VersionHistory.getVersionLabels(Version) must not return a removed label.",labels.contains(versionLabel));
    }

    public void testMoveLabel2() throws RepositoryException {
        vHistory.addVersionLabel(version.getName(), versionLabel, false);

        versionableNode.checkout();
        Version v = versionableNode.checkin();
        vHistory.addVersionLabel(v.getName(), versionLabel, true);

        List<String> labels = Arrays.asList(vHistory.getVersionLabels(v));
        assertTrue(labels.contains(versionLabel));
    }

    public void testMoveLabel3() throws RepositoryException {
        versionableNode.checkout();
        Version v = versionableNode.checkin();

        vHistory.addVersionLabel(version.getName(), versionLabel, false);
        vHistory.addVersionLabel(v.getName(), versionLabel, true);

        List<String> labels = Arrays.asList(vHistory.getVersionLabels(version));
        assertFalse(labels.contains(versionLabel));
    }

    public void testMoveLabel4() throws RepositoryException {
        versionableNode.checkout();
        Version v = versionableNode.checkin();

        vHistory.addVersionLabel(version.getName(), versionLabel, false);
        vHistory.addVersionLabel(v.getName(), versionLabel, true);

        Version v2 = vHistory.getVersionByLabel(versionLabel);
        assertTrue(v2.isSame(v));
    }
}
