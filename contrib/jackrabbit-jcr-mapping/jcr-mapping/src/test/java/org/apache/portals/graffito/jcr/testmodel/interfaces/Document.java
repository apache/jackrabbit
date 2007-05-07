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
package org.apache.portals.graffito.jcr.testmodel.interfaces;

import org.apache.portals.graffito.jcr.testmodel.inheritance.impl.DocumentStream;

public interface Document extends Content {

	/** 
	 * @see org.apache.portals.graffito.model.DocumentImpl#getContentType()
	 */
	public String getContentType();

	/**
	 * @see org.apache.portals.graffito.model.DocumentImpl#setContentType(java.lang.String)
	 */
	public void setContentType(String contentType);

	/**
	 * 
	 * @see org.apache.portals.graffito.model.DocumentImpl#getSize()
	 */
	public long getSize();

	/**
	 * 
	 * @see org.apache.portals.graffito.model.DocumentImpl#setSize(long)
	 */
	public void setSize(long size);

	public DocumentStream getDocumentStream();

	public void setDocumentStream(DocumentStream documentStream);

}
