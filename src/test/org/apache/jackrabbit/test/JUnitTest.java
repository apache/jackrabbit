/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.test;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 *
 * @version $Revision: 1.4 $, $Date: 2004/05/04 12:06:31 $
 * @author Marcel Reutegger
 */
public class JUnitTest extends TestCase {

    /** Helper object to access repository transparently */
    public static final RepositoryHelper helper = new RepositoryHelper();

    /** Logger instance for test cases */
    protected static final Logger log = Logger.getLogger(JUnitTest.class);

}
