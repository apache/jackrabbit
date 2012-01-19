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
package org.apache.jackrabbit.rmi.remote;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * Serializable {@link Xid}.
 *
 * @since Jackrabbit JCR-RMI 1.5
 */
public class SerializableXid implements Serializable, Xid {

    private final int formatId;

    private final byte[] globalTransactionId;

    private final byte[] branchQualifier;

    private final int hashCode;

    public SerializableXid(Xid xid) {
        formatId = xid.getFormatId();
        globalTransactionId = xid.getGlobalTransactionId();
        branchQualifier = xid.getBranchQualifier();
        hashCode = xid.hashCode();
    }

    public int getFormatId() {
        return formatId;
    }

    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object xid) {
        return (xid instanceof Xid)
            && formatId == ((Xid) xid).getFormatId()
            && Arrays.equals(
                    globalTransactionId, ((Xid) xid).getGlobalTransactionId())
            && Arrays.equals(
                    branchQualifier, ((Xid) xid).getBranchQualifier());
    }

}
