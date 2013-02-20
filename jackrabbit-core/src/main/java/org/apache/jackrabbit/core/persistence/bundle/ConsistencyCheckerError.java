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
package org.apache.jackrabbit.core.persistence.bundle;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.persistence.check.ReportItemImpl;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;

/**
 * Base class for errors reported by the {@link ConsistencyCheckerImpl}
 */
abstract class ConsistencyCheckerError {

    protected final String message;
    protected final NodeId nodeId;
    protected boolean repaired;

    ConsistencyCheckerError(NodeId nodeId, String message) {
        this.nodeId = nodeId;
        this.message = message;
    }

    final NodeId getNodeId() {
        return nodeId;
    }

    final void repair(final ChangeLog changes) throws ItemStateException {
        doRepair(changes);
        repaired = true;
    }

    final ReportItem getReportItem() {
        return new ReportItemImpl(nodeId.toString(), message, getType(), repaired);
    }

    /**
     * @return whether this error is repairable
     */
    abstract boolean isRepairable();

    /**
     * Repair this error and update the changelog.
     *
     * @param changes  the changelog to update with the changes made.
     * @throws ItemStateException
     */
    abstract void doRepair(final ChangeLog changes) throws ItemStateException;

    abstract ReportItem.Type getType();

    /**
     * Double check the error to eliminate false positives in live environments.
     * @return  whether the error was confirmed.
     * @throws ItemStateException
     */
    abstract boolean doubleCheck() throws ItemStateException;

    @Override
    public String toString() {
        return getType() + " - " + getNodeId();
    }
}
