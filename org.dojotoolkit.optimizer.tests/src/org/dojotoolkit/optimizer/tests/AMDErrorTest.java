/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

public class AMDErrorTest extends OptimizerTest {
	public AMDErrorTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}

	protected void runTest() throws Throwable {
		URL url = Utils.findBundle(bundleContext, "org.dojotoolkit.optimizer.servlet").getResource("/requirejs.json");
		assertNotNull(url);
		
		try {
			Map<String, Object> config = Utils.loadHandlerConfig(url);
			assertNotNull(config);
			RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);

			JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, true, config);
			optimizer.getAnalysisData(new String[] {"test/amd/error/A"});
		} catch (Exception e) {
			if (!e.getMessage().contains("Error: Unable to load src for [test/amd/error/E]. Module [test/amd/error/C] has a dependency on it.")) {
				TestCase.fail("Failure does not indicate a missing dependency");
			}
		}
	}
}
