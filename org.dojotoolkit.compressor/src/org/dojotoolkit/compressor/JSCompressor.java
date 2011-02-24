/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor;

import java.io.IOException;

/**
 * Implementations with allow callers to compress provide javascript source
 *
 */
public interface JSCompressor {
	/**
	 * @param src String value containing the javascript source
	 * @return String containing compressed javascript source
	 * @throws IOException
	 */
	String compress(String src) throws IOException;
}
