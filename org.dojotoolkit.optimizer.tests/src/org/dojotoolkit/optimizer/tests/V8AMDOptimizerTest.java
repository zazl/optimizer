/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class V8AMDOptimizerTest extends AMDOptimizerTest {
	public V8AMDOptimizerTest(BundleContext bundleContext, String[] ids, String handlerConfig) {
		super(bundleContext, ids, handlerConfig);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new AMDJSOptimizerFactory();
	}
}
