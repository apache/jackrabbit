/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.taglib.traverser;

/**
 * Depth exceeded
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce</a>
 */
class DepthExceededException extends Exception 
{

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3834024770894443576L;
    
    /**
     * 
     */
    public DepthExceededException()
    {
        super();
    }
    /**
     * @param message
     */
    public DepthExceededException(String message)
    {
        super(message);
    }
    /**
     * @param message
     * @param cause
     */
    public DepthExceededException(String message, Throwable cause)
    {
        super(message, cause);
    }
    /**
     * @param cause
     */
    public DepthExceededException(Throwable cause)
    {
        super(cause);
    }
}
