package org.apache.jackrabbit.browser.command;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.fileupload.FileItem;

public class FileItemToInputStream implements Command {

	/** file item key */
	private String fileItemKey = "fileItem";

	private String targetKey = "inputStream";

	public boolean execute(Context ctx) throws Exception {
		FileItem fileItem = (FileItem) ctx.get(fileItemKey);
		ctx.put(this.targetKey, fileItem.getInputStream());
		return false;
	}

	/**
	 * @return the fileItemKey
	 */
	public String getFileItemKey() {
		return fileItemKey;
	}

	/**
	 * @param fileItemKey
	 *            the fileItemKey to set
	 */
	public void setFileItemKey(String fileItemKey) {
		this.fileItemKey = fileItemKey;
	}

	/**
	 * @return the targetKey
	 */
	public String getTargetKey() {
		return targetKey;
	}

	/**
	 * @param targetKey the targetKey to set
	 */
	public void setTargetKey(String targetKey) {
		this.targetKey = targetKey;
	}

}
