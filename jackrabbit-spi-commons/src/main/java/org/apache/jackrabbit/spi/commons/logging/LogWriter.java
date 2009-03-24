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
package org.apache.jackrabbit.spi.commons.logging;

/**
 * A LogWriter provides methods for persisting log messages by some implementation
 * specific means. Implementations must be thread safe. That is, implementations must
 * cope with any thread concurrently calling any method of this interface at any time.
 */
public interface LogWriter {

    /**
     * Implementation specific time stamp which is logged along with each log
     * message. The values returned by this method should be monotone with respect
     * to the time they represent.
     * @return
     */
    public long systemTime();

    /**
     * Called right before a method of a SPI entity is called.
     * @param methodName  name of the method which a about to be called
     * @param args  arguments passed to the methods which is about to be called.
     */
    public void enter(String methodName, Object[] args);

    /**
     * Called right after a method of a SPI entity has been called if no
     * exception was thrown.
     * @param methodName  name of the method which has been called
     * @param args  arguments passed to the method which has been called
     * @param result  return value of the method which has been called
     */
    public void leave(String methodName, Object[] args, Object result);

    /**
     * Called right after a method of a SPI entity has been called and an
     * exception was thrown.
     * @param methodName  name of the method which has been called
     * @param args  arguments passed to the method which has been called
     * @param e  exception which was thrown by the method which has
     *   been called
     */
    public void error(String methodName, Object[] args, Exception e);
}
