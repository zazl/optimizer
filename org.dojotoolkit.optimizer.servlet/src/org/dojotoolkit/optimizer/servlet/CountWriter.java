/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.Writer;

public class CountWriter extends Writer {
	private Writer w = null;
	private int lineCount = 0;
	
	public CountWriter(Writer w) {
		this.w = w;
	}

	public void close() throws IOException {
		w.close();
	}

	public void flush() throws IOException {
		w.flush();
	}

	public void write(char[] cbuf, int off, int len) throws IOException {
		for (int i = off; i < len; i++) {
			write(cbuf[i]);
		}
	}
	
	public int getLineCount() {
		return lineCount;
	}
	
	public void write(int c) throws IOException {
		w.write(c);
		if (c == '\n') {
			lineCount++;
		}
	}
}
