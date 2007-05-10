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
package org.apache.jackrabbit.ocm.security;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

/**
 * <code>SimpleAccessManager</code> ...
 */
public class SimpleAccessManager implements AccessManager
{

	private static Logger log = Logger.getLogger(SimpleAccessManager.class);

	/**
	 * Subject whose access rights this AccessManager should reflect
	 */
	protected Subject subject;

	/**
	 * hierarchy manager used for ACL-based access control model
	 */
	protected HierarchyManager hierMgr;

	private boolean initialized;

	protected boolean system;

	protected boolean anonymous;

	/**
	 * Empty constructor
	 */
	public SimpleAccessManager()
	{
		initialized = false;
		anonymous = false;
		system = false;
	}

	//--------------------------------------------------------< AccessManager >
	/**
	 * {@inheritDoc}
	 */
	public void init(AMContext context) throws AccessDeniedException, Exception
	{
		if (initialized)
		{
			throw new IllegalStateException("already initialized");
		}

		subject = context.getSubject();
		hierMgr = context.getHierarchyManager();
		anonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
		system = !subject.getPrincipals(SystemPrincipal.class).isEmpty();

		// @todo check permission to access given workspace based on principals
		initialized = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void close() throws Exception
	{
		if (!initialized)
		{
			throw new IllegalStateException("not initialized");
		}

		initialized = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException
	{
		if (!initialized)
		{
			throw new IllegalStateException("not initialized");
		}

		if (system)
		{
			// system has always all permissions
			return;
		}
		else if (anonymous)
		{
			// anonymous is always denied WRITE & REMOVE premissions
			if ((permissions & WRITE) == WRITE || (permissions & REMOVE) == REMOVE)
			{
				throw new AccessDeniedException();
			}
		}
		// @todo check permission based on principals
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isGranted(ItemId id, int permissions) throws ItemNotFoundException, RepositoryException
	{
		if (!initialized)
		{
			throw new IllegalStateException("not initialized");
		}

		if (system)
		{
			// system has always all permissions
			return true;
		}
		else if (anonymous)
		{
			// anonymous is always denied WRITE & REMOVE premissions
			if ((permissions & WRITE) == WRITE || (permissions & REMOVE) == REMOVE)
			{
				return false;
			}
		}

		// @todo check permission based on principals
		return true;
	
		
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException
	{
		// @todo check permission to access given workspace based on principals
		return true;
	}
}
