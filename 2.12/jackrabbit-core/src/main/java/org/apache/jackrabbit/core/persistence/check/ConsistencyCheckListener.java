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
package org.apache.jackrabbit.core.persistence.check;

public interface ConsistencyCheckListener {

    /**
     * Called when checking of a node starts
     * @param id node ID
     */
    public void startCheck(String id);

    /**
     * Called when there's a consistency problem to be reported
     * @param item problem report
     */
    public void report(ReportItem item);

    /**
     * Called on errors with the check procedure
     * @param id node id (can be <code>null</code>)
     * @param message
     */
    public void error(String id, String message);

    /**
     * Called on progress with the check procedure
     * @param id node id (can be <code>null</code>)
     * @param message
     */
    public void info(String id, String message);
}
