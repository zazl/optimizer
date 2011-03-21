/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.v8;

import java.util.Map;

import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class V8JSOptimizerFactory implements JSOptimizerFactory {
	public JSOptimizer createJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum, Map<String, Object> config) {
		return new V8JSOptimizer(resourceLoader, rhinoClassLoader, javaChecksum, config);
	}
}
