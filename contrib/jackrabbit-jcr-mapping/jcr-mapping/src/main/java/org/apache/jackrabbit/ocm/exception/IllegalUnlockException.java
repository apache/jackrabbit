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
package org.apache.jackrabbit.ocm.exception;


/**
 * If user cannot unlock path, for example if he/she have not correct lockTokens
 *
 * @author Martin Koci
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class IllegalUnlockException extends LockingException {

    /** Use serialVersionUID for interoperability. */
    private final static long serialVersionUID = 5078216219061716697L;

    private final String lockOwner;

    private final String path;

    /**
     *
     * @return The JCR Lock Owner
     */
    public String getLockOwner() {
        return lockOwner;
    }

    /**
     *
     * @return the JCR path
     */
    public String getPath() {
        return path;
    }

    public IllegalUnlockException(String lockOwner, String path) {
        super();
        this.lockOwner = lockOwner;
        this.path = path;
    }

}
