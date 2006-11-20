package org.apache.jackrabbit.browser.iframeio;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

/**
 * OutputStream that writes to an underlying DataOutputStream
 */
public class IFrameIOFilterOutputStream extends ServletOutputStream {

	private DataOutputStream stream;

	public IFrameIOFilterOutputStream(OutputStream output) {
		stream = new DataOutputStream(output);
	}

	public void write(int b) throws IOException {
		stream.write(b);
	}

	public void write(byte[] b) throws IOException {
		stream.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		stream.write(b, off, len);
	}
}
