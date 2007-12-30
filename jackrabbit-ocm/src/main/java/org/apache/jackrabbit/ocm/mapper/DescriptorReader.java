package org.apache.jackrabbit.ocm.mapper;

import org.apache.jackrabbit.ocm.mapper.impl.AbstractMapperImpl;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

public interface DescriptorReader {

	/**
	 * Load all class descriptors found in an classdescriptor definition.
	 * A classdescriptor definition can be a xml config file or annotations 
	 * or another kind of resource which contain the classdescriptors 
	 * 
	 * DescriptorReader is an abstraction used to maximize reusability in {@link AbstractMapperImpl}
	 * 
	 * @return a {@link MappingDescriptor} wich will contains a collection of classdescriptors
	 * 
	 */
	public abstract MappingDescriptor loadClassDescriptors();

}