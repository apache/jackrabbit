package org.apache.jackrabbit.ocm.manager.objectconverter;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;

public interface ProxyManager {

	public abstract Object createBeanProxy(Session session,
			ObjectConverter objectConverter, Class beanClass, String path);

	public abstract Object createCollectionProxy(Session session,
			CollectionConverter collectionConverter, Node parentNode,
			CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass);

}