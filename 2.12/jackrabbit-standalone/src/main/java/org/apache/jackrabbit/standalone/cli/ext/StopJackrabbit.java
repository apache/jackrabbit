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
package org.apache.jackrabbit.standalone.cli.ext;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Stop Jackrabbit
 */
public class StopJackrabbit implements Command {

    /** logger */
    private static Log log = LogFactory.getLog(StopJackrabbit.class);

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("stopping jackrabbit");
        }
        RepositoryImpl repo = (RepositoryImpl) CommandHelper.getRepository(ctx);
        if (repo == null) {
            throw new IllegalStateException("No current working repository");
        }
        repo.shutdown();
        CommandHelper.setRepository(ctx, null, null);
        return false;
    }
}
