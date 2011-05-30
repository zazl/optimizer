/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor.uglifyjs.v8;

import org.dojotoolkit.compressor.JSCompressor;
import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.compressor.JSCompressorFactoryImpl;
import org.dojotoolkit.server.util.resource.ResourceLoader;

public class V8UglifyJSCompressorFactory implements JSCompressorFactory {

	public JSCompressor createJSCompressor(ResourceLoader resourceLoader) {
		return new V8UglifyJSCompressor(resourceLoader);
	}

	public String[] getIgnoreList() {
		return JSCompressorFactoryImpl.defaultIgnoreList;
	}
}
