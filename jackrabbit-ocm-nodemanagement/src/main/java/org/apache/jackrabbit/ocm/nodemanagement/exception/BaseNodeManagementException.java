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
package org.apache.jackrabbit.ocm.nodemanagement.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/** Base exception for all JCR Node Type Management exceptions.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class BaseNodeManagementException extends Exception
{

    /** Root exception.
     */
    private Exception wrappedException;

    /** Creates a new instance of BaseNodeManagementException. */
    public BaseNodeManagementException()
    {
    }

    /** Creates a new instance of BaseNodeManagementException.
     * @param message Exception message
     */
    public BaseNodeManagementException(String message)
    {
        super(message);
    }

    /** Creates a new instance of BaseNodeManagementException.
     * @param rootException Root Exception
     */
    public BaseNodeManagementException(Exception rootException)
    {
        setWrappedException(rootException);
    }

    /** Getter for property wrappedException.
     *
     * @return wrappedException
     */
    public Exception getWrappedException()
    {
        return wrappedException;
    }

    /** Setter for property wrappedException.
     *
     * @param object wrappedException
     */
    public void setWrappedException(Exception object)
    {
        this.wrappedException = object;
    }

    public void printStackTrace( PrintStream ps )
    {
        if ( getWrappedException() == null || getWrappedException() == this )
        {
            super.printStackTrace( ps );
        }
        else
        {
            ps.println( this );
            getWrappedException().printStackTrace( ps );
        }
    }

    public void printStackTrace( PrintWriter pw )
    {
        if ( getWrappedException() == null || getWrappedException() == this )
        {
            super.printStackTrace( pw );
        }
        else
        {
            pw.println( this );
            getWrappedException().printStackTrace( pw );
        }
    }
}
