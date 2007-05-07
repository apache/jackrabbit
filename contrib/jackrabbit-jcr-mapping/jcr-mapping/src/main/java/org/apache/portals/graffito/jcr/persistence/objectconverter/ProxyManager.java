package org.apache.portals.graffito.jcr.persistence.objectconverter;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.portals.graffito.jcr.mapper.model.CollectionDescriptor;
import org.apache.portals.graffito.jcr.persistence.collectionconverter.CollectionConverter;

public interface ProxyManager {

	public abstract Object createBeanProxy(Session session,
			ObjectConverter objectConverter, Class beanClass, String path);

	public abstract Object createCollectionProxy(Session session,
			CollectionConverter collectionConverter, Node parentNode,
			CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass);

}