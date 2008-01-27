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
package org.apache.jackrabbit.core.cluster;

/**
 * Cluster operation that will be written to the cluster's journal and
 * ultimately processed by other instances.
 */
public interface ClusterOperation {

    /**
     * Called when the operation has been finished. The passed flag indicates
     * whether or not the operation was successful.
     *
     * @param successful <code>true</code> if the operation ended successfully;
     *                   <code>false</code> otherwise
     */
    void ended(boolean successful);

}
