/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

public abstract class AMDOptimizerTest extends OptimizerTest {
	private static String depCompare = "dojo/_base/_loader/bootstrap.js dojo/lib/backCompat.js dojo/_base/_loader/hostenv_browser.js dojo/lib/kernel.js dojo/_base/lang.js dojo/_base/array.js dojo/_base/declare.js dojo/_base/connect.js dojo/_base/Deferred.js dojo/_base/json.js dojo/_base/Color.js dojo/_base/window.js dojo/_base/event.js dojo/_base/html.js dojo/_base/NodeList.js dojo/_base/query.js dojo/_base/xhr.js dojo/_base/fx.js dojo/lib/main-browser.js dijit/lib/main.js dijit/_base/manager.js dojo/Stateful.js dijit/_WidgetBase.js dojo/window.js dijit/_base/focus.js dojo/AdapterRegistry.js dijit/_base/place.js dijit/_base/window.js dijit/_base/popup.js dijit/_base/scroll.js dojo/uacss.js dijit/_base/sniff.js dijit/_base/typematic.js dijit/_base/wai.js dijit/_base.js dijit/_Widget.js dojo/string.js dojo/date/stamp.js dojo/parser.js dojo/cache.js dijit/_Templated.js dijit/_Container.js dijit/_Contained.js dijit/layout/_LayoutWidget.js dojo/regexp.js dojo/cookie.js dijit/_CssStateMixin.js dijit/form/_FormWidget.js dijit/_HasDropDown.js dijit/form/Button.js dijit/form/ToggleButton.js dijit/layout/StackController.js dijit/layout/StackContainer.js dijit/layout/_TabContainerBase.js dijit/_KeyNavContainer.js dijit/MenuItem.js dijit/PopupMenuItem.js dijit/CheckedMenuItem.js dijit/MenuSeparator.js dijit/Menu.js dijit/layout/TabController.js dijit/layout/ScrollingTabController.js dijit/layout/TabContainer.js "; 
	private String handlerConfig = null;
	
	public AMDOptimizerTest(BundleContext bundleContext, String[] ids, String handlerConfig) {
		super(bundleContext, ids);
		this.handlerConfig = handlerConfig;
	}

	protected void runTest() throws Throwable {
		URL url = Utils.findBundle(bundleContext, "org.dojotoolkit.optimizer.servlet").getResource(handlerConfig);
		assertNotNull(url);
		
		try {
			Map<String, Object> config = Utils.loadHandlerConfig(url);
			assertNotNull(config);
			RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
			
			JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, config, new java.io.File("."));
			JSAnalysisData  analysisData = optimizer.getAnalysisData(new String[] {"dijit/layout/TabContainer"});
			StringBuffer sb = new StringBuffer();
			for (String dependency: analysisData.getDependencies()) {
				sb.append(dependency);
				sb.append(' ');
			}
			assertEquals(depCompare, sb.toString());
		} catch (IOException e) {
			TestCase.fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
