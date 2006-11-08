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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.log4j.Logger;

/**
 * SizeCalculator for testing purposes.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class SimpleSizeCalculator extends AbstractSizeCalculator
{
    /** Logger */
	private static Logger log = Logger.getLogger(SimpleSizeCalculator.class);

    /**
     * Constructor
     */
    public SimpleSizeCalculator()
    {
        super();
    }
    
    /** @inheritDoc */
    public double getSize(Node node)
    {
        double size = 0;
        try
        {
            PropertyIterator iter = node.getProperties();
            while (iter.hasNext())
            {
                Property prop = iter.nextProperty();
                size = size + this.getSize(prop);
            }
        } catch (RepositoryException e)
        {
            log.error("Unable to get properties from node. " + e.getMessage(),
                    e);
        }
        return size;
    }

    /** @inheritDoc */
    public double getSize(Property property)
    {
        long bits = 0;
        try
        {
            switch (property.getType())
            {
            case PropertyType.BINARY:
                bits = this.getBinarySize(property) ;
                break;
            case PropertyType.BOOLEAN:
                if (property.getDefinition().isMultiple()) {
                    bits = property.getValues().length * 1 ;
                } else {
                    bits = 1 ;
                }
                break;
            case PropertyType.DOUBLE:
            case PropertyType.DATE:
            case PropertyType.LONG:
                if (property.getDefinition().isMultiple()) {
                    bits = property.getValues().length * 64 ;
                } else {
                    bits = 64 ;
                }
                break;
            default:
                bits = this.getStringSize(property);
                break;
            }
        } catch (RepositoryException e)
        {
            log.error("Unable to get values from property. " + e.getMessage(),
                    e);
        }
        return this.convert(bits, this.unit);
    }

    /**
     * @param property
     * @return the size in bits of a binary property
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    private long getBinarySize(Property prop) throws ValueFormatException, RepositoryException {
        if (prop.getDefinition().isMultiple()) {
            long[] sizes = prop.getLengths() ;
            long size = 0 ;
            for (int i = 0; i < sizes.length; i++)
            {
                size = size + sizes[i];
            }
            return size * 8 ;
        } else {
            return prop.getLength() * 8;
        }
    }
    
    /**
     * @param prop
     * @return size in bits
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    private long getStringSize(Property prop) throws ValueFormatException, RepositoryException {
        if (prop.getDefinition().isMultiple()) {
            long[] sizes = prop.getLengths() ;
            long size = 0 ;
            for (int i = 0; i < sizes.length; i++)
            {
                size = size + sizes[i] * 16 ;
            }
            return size;
        } else {
            return prop.getLength() * 16 ;
        }
    }
    
}