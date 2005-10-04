package org.apache.jackrabbit.command.xml;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Serializes the node to the given file using the System View Format
 */
public class ExportSysViewToFile extends AbstractExportViewToFile
{

	/**
	 * @inheritDoc
	 */
	protected void exportView(Node node, OutputStream out, boolean skipBinary,
			boolean noRecurse) throws PathNotFoundException, IOException,
			RepositoryException
	{
		node.getSession().exportSystemView(node.getPath(), out, skipBinary,
				noRecurse);
	}

}
