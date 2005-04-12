/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.search;

import java.io.IOException;

/**
 * Implements default behaviour for some methods of {@link QueryHandler}.
 */
public abstract class AbstractQueryHandler implements QueryHandler {

    /**
     * The context for this query handler.
     */
    private QueryHandlerContext context;

    /**
     * Initializes this query handler by setting all properties in this class
     * with appropriate parameter values.
     *
     * @param context the context for this query handler.
     */
    public final void init(QueryHandlerContext context) throws IOException {
        this.context = context;
        doInit();
    }

    /**
     * This method must be implemented by concrete sub classes and will be
     * called from {@link #init}.
     */
    protected abstract void doInit() throws IOException;

    /**
     * Returns the context for this query handler.
     *
     * @return the <code>QueryHandlerContext</code> instance for this
     *         <code>QueryHandler</code>.
     */
    public QueryHandlerContext getContext() {
        return context;
    }
}
