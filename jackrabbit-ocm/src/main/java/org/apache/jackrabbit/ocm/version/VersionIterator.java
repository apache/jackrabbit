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

package org.apache.jackrabbit.ocm.version;

import java.util.Iterator;

import javax.jcr.version.Version;


/**
 * VersionIterator is a wrapper class for JCR VersionIterator
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class VersionIterator implements Iterator
{

	private javax.jcr.version.VersionIterator versionIterator;
	
	public VersionIterator(javax.jcr.version.VersionIterator versionIterator)
	{
		this.versionIterator = versionIterator;
	}

	/**
	 *
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		return versionIterator.hasNext();
	}

	/**
	 *
	 * @see java.util.Iterator#next()
	 */
	public Object next()
	{

		try
		{
			Version version =  versionIterator.nextVersion();
			return new org.apache.jackrabbit.ocm.version.Version(version);
		}
		catch (Exception e)
		{
           return null;			
		}

	}

	/**
	 *
	 * @return the versionIterator size
	 */
	public long getSize()
	{
	   return versionIterator.getSize();	
	}
	
	/**
	 *
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
		versionIterator.remove();
		
	}

}
