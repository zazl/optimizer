package org.dojotoolkit.optimizer.amd.rhino;

import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class AMDJSOptimizerFactory implements JSOptimizerFactory {

	public JSOptimizer createJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum) {
		return new AMDJSOptimizer(resourceLoader, rhinoClassLoader, javaChecksum);
	}
}
