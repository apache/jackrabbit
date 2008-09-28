package org.apache.jackrabbit.ocm.testmodel;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
@Node
public class MultiValueWithObjectCollection {
	/**
	 *
	 * Simple object used to test multivalue properties
	 *
	 * @author <a href="mailto:boni.g@bioimagene.com"> Boni Gopalan </a>
	 * @version $Id: Exp $
	 */
		@Field(path=true) private String path;
		
		@Field private String name;
		
		@Collection(elementClassName=Object.class,  collectionConverter=MultiValueCollectionConverterImpl.class)
		private java.util.Collection multiValues;

		@Collection(elementClassName=Object.class,  collectionConverter=MultiValueCollectionConverterImpl.class)
		private java.util.Collection nullMultiValues;

		
		
		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * @return Returns the name.
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * @param name The name to set.
		 */
		public void setName(String name)
		{
			this.name = name;
		}

		/**
		 * @return Returns the multiValues.
		 */
		public java.util.Collection getMultiValues()
		{
			return multiValues;
		}

		/**
		 * @param multiValues
		 *            The multiValues to set.
		 */
		public void setMultiValues(java.util.Collection multiValues)
		{
			this.multiValues = multiValues;
		}

		/**
		 * @return Returns the nullMultiValues.
		 */
		public java.util.Collection getNullMultiValues()
		{
			return nullMultiValues;
		}

		/**
		 * @param nullMultiValues
		 *            The nullMultiValues to set.
		 */
		public void setNullMultiValues(java.util.Collection nullMultiValues)
		{
			this.nullMultiValues = nullMultiValues;
		}

}
