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
package org.apache.jackrabbit.taglib.size;

import org.apache.log4j.Logger;

/**
 * Superclass of SizeCalculator 
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce</a>
 */
public abstract class AbstractSizeCalculator implements SizeCalculator
{
    /** Logger */
	private static Logger log = Logger.getLogger(AbstractSizeCalculator.class);

    /** Unit */
    protected int unit = SizeCalculator.BITS;

    /**
     * Unit conversion
     * @param bits
     * @param unit
     * @return
     */
    protected double convert(long size, int unit) {
        switch (unit)
        {
            case SizeCalculator.BITS:
                return size ;
            case SizeCalculator.BYTES:
                return size / 8d ;
            case SizeCalculator.KILOBYTES:
                return size / 8192d ;
            case SizeCalculator.MEGABYTES:
                return size / 8388608d ;
            case SizeCalculator.GIGABYTES:
                return size / 8589934592d ;
            default:
                String msg = "No such unit." + unit ;
                log.error(msg);
            	throw new IllegalArgumentException(msg) ;
        }
    }

    /** @inheritDoc */
    public void setUnit(int unit)
    {
        this.unit = unit;
    }
    
}
