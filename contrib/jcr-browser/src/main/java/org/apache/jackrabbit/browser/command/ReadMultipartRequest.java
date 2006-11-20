package org.apache.jackrabbit.browser.command;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.jackrabbit.command.web.JcrServletWebContext;

/**
 * Parses a Multipart request and puts every item in the context
 */
public class ReadMultipartRequest implements Command {

	/** max upload size */
	private String maxSize = "3000000";

	@SuppressWarnings("unchecked")
	public boolean execute(Context ctx) throws Exception {
		// Configure file upload
		JcrServletWebContext servletCtx = (JcrServletWebContext) ctx;
		FileItemFactory itemFactory = new DiskFileItemFactory();
		ServletFileUpload fileUpload = new ServletFileUpload(itemFactory);
		fileUpload.setSizeMax(Integer.valueOf(this.maxSize));

		// parse request
		List items = fileUpload.parseRequest(servletCtx.getRequest());
		Iterator iter = items.iterator();
		while (iter.hasNext()) {
			FileItem item = (FileItem) iter.next();
			if (item.isFormField()) {
				ctx.put(item.getFieldName(), item.getString());
			} else {
				ctx.put(item.getFieldName(), item);
			}
		}

		return false;
	}

	/**
	 * @return the maxSize
	 */
	public String getMaxSize() {
		return maxSize;
	}

	/**
	 * @param maxSize the maxSize to set
	 */
	public void setMaxSize(String maxSize) {
		this.maxSize = maxSize;
	}

}
