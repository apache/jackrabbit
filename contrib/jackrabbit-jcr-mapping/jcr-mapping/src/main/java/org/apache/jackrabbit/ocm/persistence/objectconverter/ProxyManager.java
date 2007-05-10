package org.apache.jackrabbit.ocm.persistence.objectconverter;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.CollectionConverter;

public interface ProxyManager {

	public abstract Object createBeanProxy(Session session,
			ObjectConverter objectConverter, Class beanClass, String path);

	public abstract Object createCollectionProxy(Session session,
			CollectionConverter collectionConverter, Node parentNode,
			CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass);

}