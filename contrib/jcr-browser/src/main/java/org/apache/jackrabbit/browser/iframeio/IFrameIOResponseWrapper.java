package org.apache.jackrabbit.browser.iframeio;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class IFrameIOResponseWrapper extends HttpServletResponseWrapper {

	private ByteArrayOutputStream output;

	private int contentLength;

	private String contentType;

	public IFrameIOResponseWrapper(HttpServletResponse response) {
		super(response);
		output = new ByteArrayOutputStream();
	}

	public byte[] getData() {
		return output.toByteArray();
	}

	public ServletOutputStream getOutputStream() {
		return new IFrameIOFilterOutputStream(output);
	}

	public void setContentLength(int length) {
		this.contentLength = length;
		super.setContentLength(length);
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentType(String type) {
		this.contentType = type;
		super.setContentType(type);
	}

	public String getContentType() {
		return contentType;
	}

	public PrintWriter getWriter() {
		return new PrintWriter(getOutputStream(), true);
	}

}
