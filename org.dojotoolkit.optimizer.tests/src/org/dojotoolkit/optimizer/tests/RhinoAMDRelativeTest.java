package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.amd.rhino.AMDJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class RhinoAMDRelativeTest extends AMDRelativeTest {
	public RhinoAMDRelativeTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new AMDJSOptimizerFactory();
	}
}