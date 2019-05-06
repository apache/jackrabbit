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
package org.apache.jackrabbit.server.io;

/**
 * <code>IOContext</code> defines the common methods for {@link ImportContext}
 * and {@link ExportContext}
 */
public interface IOContext {

    /**
     * Returns the IOListener.
     */
    public IOListener getIOListener();

    /**
     * Return true if the given export context can provide an output stream
     */
    public boolean hasStream();

    /**
     * Informs this context that it will not be used for further exports any
     * more. A boolean flag indicates about the success of the export.
     */
    public void informCompleted(boolean success);

    /**
     * Returns true if this context already has been completed.
     *
     * @return true if this context already has been completed.
     */
    public boolean isCompleted();
}