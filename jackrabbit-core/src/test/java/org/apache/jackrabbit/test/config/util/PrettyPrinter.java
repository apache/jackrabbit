/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.test.config.util;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * <code>PrettyPrinter</code> ...
 *
 */
public class PrettyPrinter extends PrintWriter {
	
	private final int indent;
	
	private int currentIndent;
	
	private final String indentString;

	public PrettyPrinter(OutputStream out) {
		super(out);
		this.indent = 4;
		this.indentString = " ";
	}
	
	public PrettyPrinter(OutputStream out, int indent, String indentString) {
		super(out);
		this.indent = indent;
		this.indentString = indentString;
	}
	
	public void increaseIndent() {
		currentIndent += indent;
	}

	public void decreaseIndent() {
		if ((currentIndent - indent) < 0) {
			currentIndent = 0;
		} else {
			currentIndent -= indent;
		}
	}
	
	public void printIndent() {
		for (int i=0; i < currentIndent; i++) {
			print(indentString);
		}
	}

	public void printIndent(String s) {
		printIndent();
		super.print(s);
	}

	public void printlnIndent(String x) {
		printIndent();
		super.println(x);
		flush();
	}

}
