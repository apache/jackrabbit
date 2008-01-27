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
package org.apache.jackrabbit.ocm.lock;

import javax.jcr.Node;

import org.apache.jackrabbit.ocm.exception.RepositoryException;

/**
 * Wrapper class for a JCR Lock object
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class Lock
{

	private javax.jcr.lock.Lock lock;

	public Lock(javax.jcr.lock.Lock lock)
	{
		this.lock = lock;
	}

	public String getLockOwner() {
		return lock.getLockOwner();
	}

	public String getLockToken() {
		return lock.getLockToken();
	}

	public Node getNode() {
		return lock.getNode();
	}

	public boolean isDeep() {
		return lock.isDeep();
	}

	public boolean isLive()  {
		try
		{
		   return lock.isLive();
		}
		catch (javax.jcr.RepositoryException e)
		{
			throw new RepositoryException(e);
		}
	}

	public boolean isSessionScoped() {
		return lock.isSessionScoped();
	}

	public void refresh() {
		try
		{
		   lock.refresh();
		}
		catch (javax.jcr.RepositoryException e)
		{
			throw new RepositoryException(e);
		}
		
	}

	
}
