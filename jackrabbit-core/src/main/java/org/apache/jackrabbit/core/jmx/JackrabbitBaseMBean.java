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
package org.apache.jackrabbit.core.jmx;

/**
 * http://www.oracle.com/technetwork/java/javase/tech/best-practices-jsp-136021.
 * html
 * 
 * This should be the base for any MBean Services built for Jackrabbit
 * 
 */
public interface JackrabbitBaseMBean {

    String BASE_NAME = "org.apache.jackrabbit";

    /**
     * If this service is currently registering stats
     * 
     * @return <code>true</code> if the service is enabled
     */
    boolean isEnabled();

    /**
     * Enables/Disables the service
     * 
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * clears all data
     */
    void reset();

}
