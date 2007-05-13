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
package org.apache.jackrabbit.ocm.testmodel.inheritance.impl;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Document;

/**
 * CMS VersionnedDocument implementation.
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * 
 */
public class DocumentImpl extends ContentImpl implements Document
{
    protected final static Log log =  LogFactory.getLog(DocumentImpl.class);
    
    protected long size;
    protected String contentType;   
        
    protected DocumentStream documentStream;


    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#getContentType()
     */
    public String getContentType()
    {
        return this.contentType;
    }

    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#setContentType(java.lang.String)
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#getSize()
     */
    public long getSize()
    {
        return size;
    }

    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#setSize(long)
     */
    public void setSize(long size)
    {
        this.size = size;
    }

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#getDocumentStream()
	 */
	public DocumentStream getDocumentStream() {
		return documentStream;
	}

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Document#setDocumentStream(org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentStream)
	 */
	public void setDocumentStream(DocumentStream documentStream) {
		this.documentStream = documentStream;
	}

    
}

