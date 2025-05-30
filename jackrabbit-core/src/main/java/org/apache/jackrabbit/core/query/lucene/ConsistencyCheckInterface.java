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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.util.List;

/**
 * Interface for consistency checking of search indexes.
 * 
 * @since Apache Jackrabbit 2.0
 */
public interface ConsistencyCheckInterface {
    
    /**
     * Runs the consistency check.
     * 
     * @throws IOException if an error occurs while running the check.
     */
    void run() throws IOException;
    
    /**
     * Repairs detected errors during the consistency check.
     * 
     * @param ignoreFailure if <code>true</code> repair failures are ignored,
     *   the repair continues without throwing an exception. If
     *   <code>false</code> the repair procedure is aborted on the first
     *   repair failure.
     * @throws IOException if a repair failure occurs.
     */
    void repair(boolean ignoreFailure) throws IOException;
    
    /**
     * Returns the errors detected by the consistency check.
     * 
     * @return the errors detected by the consistency check.
     */
    List<ConsistencyCheckError> getErrors();
    
    /**
     * Performs a double check on the detected errors to verify they are still valid.
     */
    void doubleCheckErrors();
}
