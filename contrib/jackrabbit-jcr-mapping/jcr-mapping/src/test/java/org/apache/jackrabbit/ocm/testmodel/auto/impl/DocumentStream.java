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
package org.apache.jackrabbit.ocm.testmodel.auto.impl;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 * Document Content
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * 
 */

@Node(jcrType="ocm:documentstream", discriminator=false)
public class DocumentStream 
{
    protected final static Log log = LogFactory.getLog(DocumentStream.class);
      
    @Field( jcrName="ocm:binarycontent") protected byte[] content;    
    @Field( jcrName="ocm:encoding") protected String encoding;    
    @Field(path=true) protected String path;


    /**
     * @return Returns the content.
     */
    public InputStream getContentStream()
    {
        return new ByteArrayInputStream(content);        
    }

    /**
     * @return Returns the content.
     */
    public byte[] getContent()
    {
        
        return content;
    }


    /**
     * @param stream The content to set.
     */
    public void setContent(byte[] stream)
    {
        
        content = stream;

    }
        
    /**
     * @return Returns the encoding.
     */
    public String getEncoding()
    {
        return encoding;
    }
    
    /**
     * @param encoding The encoding to set.
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
    
    
}


