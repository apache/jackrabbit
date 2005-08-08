/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.transaction;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * <code>TransactionInfo</code> class encapsultes the information present
 * in the {@link #XML_TRANSACTIONINFO} element that forms the request body of
 * the UNLOCk request for a transaction lock.
 *
 * @see TransactionConstants#XML_TRANSACTIONINFO
 * @see TransactionConstants#XML_TRANSACTION
 */
public class TransactionInfo implements TransactionConstants {

    private static Logger log = Logger.getLogger(TransactionInfo.class);

    private Element status;

    /**
     * Creates a <code>TransactionInfo</code> object from the given 'transactionInfo'
     * element. The 'transactionInfo' must have the following form:
     * <pre>
     *
     *  &lt;!ELEMENT transactioninfo (transactionstatus) &gt;
     *  &lt;!ELEMENT transactionstatus ( commit | rollback ) &gt;
     *  &lt;!ELEMENT commit EMPTY &gt;
     *  &lt;!ELEMENT rollback EMPTY &gt;
     * </pre>
     * @param transactionInfo as present in the UNLOCK request body.
     * @throws IllegalArgumentException if the given transactionInfo element
     * is not valid.
     */
    public TransactionInfo(Element transactionInfo) {
        if (transactionInfo == null || !XML_TRANSACTIONINFO.equals(transactionInfo.getName())) {
            throw new IllegalArgumentException("transactionInfo element expected.");
        }
        Element tStatus = transactionInfo.getChild(XML_TRANSACTIONSTATUS, NAMESPACE);
        if (tStatus == null) {
            throw new IllegalArgumentException("transactionInfo must contain a single 'transactionstatus' element.");
        }

        // retrieve status: commit or rollback
        status = tStatus.getChild(XML_COMMIT, NAMESPACE);
        if (status == null) {
            status = tStatus.getChild(XML_ROLLBACK, NAMESPACE);
        }

        if (status == null) {
            throw new IllegalArgumentException("'jcr:transactionstatus' element must contain either a '" + XML_COMMIT + "' or a '" + XML_ROLLBACK + "' elements.");
        }
    }

    /**
     * Returns either 'commit' or 'rollback' with are the only allowed status
     * types.
     *
     * @return 'commit' or 'rollback'
     * @see #XML_COMMIT
     * @see #XML_ROLLBACK
     */
    public String getStatus() {
        return status.getName();
    }
}