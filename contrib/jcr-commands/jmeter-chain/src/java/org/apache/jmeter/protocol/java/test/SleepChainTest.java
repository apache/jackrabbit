/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jmeter.protocol.java.test;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

/**
 * <p>
 * The <code>SleepChainTest</code> class is a simple example class of a Chain
 * command for a JMeter Java protocol client. The class implements the
 * <code>Command</code> interface.
 * </p>
 * <p>
 * During each sample, this client will sleep for some amount of time. The
 * amount of time to sleep is determined from the single parameter SleepTime
 * </p>
 * 
 */
public class SleepChainTest implements Command
{
    /** time to sleep */
    private long time = 1000 ;

    public boolean execute(Context arg0) throws Exception
    {
        Thread.sleep(time);
        return false;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }
}
