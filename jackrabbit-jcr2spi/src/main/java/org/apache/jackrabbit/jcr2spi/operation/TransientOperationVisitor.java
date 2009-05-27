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
package org.apache.jackrabbit.jcr2spi.operation;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * <code>TransientOperationVisitor</code>...
 */
public abstract class TransientOperationVisitor implements OperationVisitor {

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Clone)
     */
    public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Clone isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Copy)
     */
    public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Copy  isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Checkout)
     */
    public void visit(Checkout operation) throws RepositoryException, UnsupportedRepositoryOperationException {
        throw new UnsupportedOperationException("Internal error: Checkout isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Checkin)
     */
    public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Checkin isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Checkpoint)
     */
    public void visit(Checkpoint operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Checkin isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Update)
     */
    public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Update isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Restore)
     */
    public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Restore isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Merge)
     */
    public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Merge isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(ResolveMergeConflict)
     */
    public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: ResolveMergeConflict isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockOperation)
     */
    public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Lock isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockRefresh)
     */
    public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRefresh isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockRelease)
     */
    public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRelease isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(AddLabel)
     */
    public void visit(AddLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: AddLabel isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(RemoveLabel)
     */
    public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveLabel isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(RemoveVersion)
     */
    public void visit(RemoveVersion operation) throws VersionException, AccessDeniedException, ReferentialIntegrityException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveVersion isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(WorkspaceImport)
     */
    public void visit(WorkspaceImport operation) throws RepositoryException {
        throw new UnsupportedOperationException("Internal error: WorkspaceImport isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(CreateActivity)
     */
    public void visit(CreateActivity operation) throws RepositoryException {
        throw new UnsupportedOperationException("Internal error: CreateActivity isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(RemoveActivity)
     */
    public void visit(RemoveActivity operation) throws RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveActivity isn't a transient operation.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(CreateConfiguration)
     */
    public void visit(CreateConfiguration operation) throws RepositoryException {
        throw new UnsupportedOperationException("Internal error: CreateConfiguration isn't a transient operation.");
    }
}