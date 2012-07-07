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
package org.apache.jackrabbit.core.util.db;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamWrapper {

    static Logger log = LoggerFactory.getLogger(StreamWrapper.class);

    private InputStream stream;
    private final long size;

    /**
     * Creates a wrapper for the given InputStream that can
     * safely be passed as a parameter to the {@link ConnectionHelper#exec(String, Object...)},
     * {@link ConnectionHelper#exec(String, Object[], boolean, int)} and
     * {@link ConnectionHelper#update(String, Object[])} methods.
     * If the wrapped Stream is a {@link TempFileInputStream} it will be wrapped again by a {@link BufferedInputStream}.
     * 
     * @param in the InputStream to wrap
     * @param size the size of the input stream
     */
    public StreamWrapper(InputStream in, long size) {
        this.stream = in;
        this.size = size;
    }
    
    public InputStream getStream() {
        if (stream instanceof TempFileInputStream) {
            return new BufferedInputStream(stream);
        }
        return stream;
    }
    
    public long getSize() {
        return size;
    }

    /**
     * Cleans up the internal Resources
     */
	public void cleanupResources() {
        if (stream instanceof TempFileInputStream) {
        	try {
        		stream.close();
        		((TempFileInputStream) stream).deleteFile();
        	} catch (IOException e) {
        		log.warn("Unable to cleanup the TempFileInputStream");
        	}
        }
	}

    /**
     * Resets the internal InputStream that it could be re-read.<br>
     * Is used from {@link RetryManager} if a {@link SQLException} has occurred.<br>
     * At the moment only a {@link TempFileInputStream} can be reseted.
     * 
     * @return returns true if it was able to reset the Stream
     */
    public boolean resetStream() {
    	if (stream instanceof TempFileInputStream) {
    		try {
	    		TempFileInputStream tempFileInputStream = (TempFileInputStream) stream;
	    		// Close it if it is not already closed ...
	    		tempFileInputStream.close();
    			stream = new TempFileInputStream(tempFileInputStream.getFile(), true);
    			return true;
    		} catch (Exception e) {
    			log.warn("Failed to create a new TempFileInputStream", e);
    		}
    	}
    	return false;
	}
}
