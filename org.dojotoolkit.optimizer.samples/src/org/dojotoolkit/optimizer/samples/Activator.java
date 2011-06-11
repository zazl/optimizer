/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.samples;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.osgi.OSGiResourceLoader;
import org.dojotoolkit.optimizer.servlet.JSHandler;
import org.dojotoolkit.optimizer.servlet.JSServlet;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.eclipse.equinox.http.registry.HttpContextExtensionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private ServiceTracker httpServiceTracker = null;
	private ServiceTracker httpContextExtensionServiceTracker = null;
	private ServiceTracker jsCompressorFactoryTracker = null;
	private ServiceTracker jsOptimizerFactoryServiceTracker = null;
	private BundleContext context = null;
	private boolean servletRegistered = false;
	private HttpService httpService = null;
	private ServiceReference httpServiceReference = null;
	private HttpContextExtensionService httpContextExtensionService = null;
	private JSCompressorFactory jsCompressorFactory = null;
	private JSOptimizerFactory jsOptimizerFactory = null;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		httpServiceTracker = new HttpServiceTracker(context);
		httpServiceTracker.open();
		httpContextExtensionServiceTracker = new HttpContextExtensionServiceTracker(context);
		httpContextExtensionServiceTracker.open();
		boolean useV8 = Boolean.valueOf(System.getProperty("V8", "false"));
		String compressorType = System.getProperty("compressorType");
		jsCompressorFactoryTracker = new JSCompressorFactoryServiceTracker(context, useV8, compressorType);
		jsCompressorFactoryTracker.open();
		jsOptimizerFactoryServiceTracker = new JSOptimizerFactoryServiceTracker(context, useV8, System.getProperty("jsHandlerType"));
		jsOptimizerFactoryServiceTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		httpContextExtensionServiceTracker.close();
		httpContextExtensionServiceTracker = null;
		httpServiceTracker.close();
		httpServiceTracker = null;
		jsCompressorFactoryTracker.close();
		jsCompressorFactoryTracker = null;
		jsOptimizerFactoryServiceTracker.close();
		jsOptimizerFactoryServiceTracker = null;
		this.context = null;
	}
	
	private void registerServlet() {
		if (!servletRegistered && httpService != null && httpContextExtensionService != null && jsCompressorFactory != null && jsOptimizerFactory != null) {
			HttpContext httpContext = httpContextExtensionService.getHttpContext(httpServiceReference, "org.dojotoolkit.optimizer.samples.httpcontext");
			if (httpContext == null) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				httpContext = httpContextExtensionService.getHttpContext(httpServiceReference, "org.dojotoolkit.optimizer.samples.httpcontext");
				if (httpContext == null) {
					System.out.println("Unable to obtain HttpContext for org.dojotoolkit.optimizer.samples.httpcontext");
					return;
				}
			}
			boolean javaChecksum = Boolean.valueOf(System.getProperty("javaChecksum", "false"));
			List<String> bundleIdList = new ArrayList<String>();
			String bundleIdsString = System.getProperty("searchBundleIds");
			if (bundleIdsString != null) {
				StringTokenizer st = new StringTokenizer(bundleIdsString, ",");
				while (st.hasMoreTokens()) {
					bundleIdList.add(st.nextToken().trim());
				}
			}
			String[] bundleIds = new String[bundleIdList.size()];
			bundleIds = bundleIdList.toArray(bundleIds);
			ResourceLoader resourceLoader = new OSGiResourceLoader(context, bundleIds, jsCompressorFactory);
			RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
			String jsHandlerType = System.getProperty("jsHandlerType");
			
			JSServlet jsServlet = new JSServlet(resourceLoader, jsOptimizerFactory, rhinoClassLoader, javaChecksum, jsHandlerType, null);
			try {
				httpService.registerServlet("/_javascript", jsServlet, null, httpContext);
				httpService.registerServlet("/", new ResourceServlet(resourceLoader), null, httpContext);
				servletRegistered = true;
			} catch (ServletException e) {
				e.printStackTrace();
			} catch (NamespaceException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class HttpServiceTracker extends ServiceTracker {
		public HttpServiceTracker(BundleContext context) {
			super(context, HttpService.class.getName(), null);
		}

		public Object addingService(ServiceReference reference) {
			httpServiceReference = reference;
			httpService = (HttpService) context.getService(reference);
			registerServlet();
			return httpService;
		}

		public void removedService(ServiceReference reference, Object service) {
			final HttpService httpService = (HttpService) service;
			if (servletRegistered) {
				httpService.unregister("/_javascript");
				httpService.unregister("/");
			}
			super.removedService(reference, service);
		}			
	}
	
	private class HttpContextExtensionServiceTracker extends ServiceTracker {
		public HttpContextExtensionServiceTracker(BundleContext context) {
			super(context, HttpContextExtensionService.class.getName(), null);
		}
		
		public Object addingService(ServiceReference reference) {
			httpContextExtensionService = (HttpContextExtensionService)context.getService(reference);
			registerServlet();
			return httpContextExtensionService;
		}
	}
	
	private class JSCompressorFactoryServiceTracker extends ServiceTracker {
		private boolean useV8 = false; 
		private String compressorType = null;
		
		public JSCompressorFactoryServiceTracker(BundleContext context, boolean useV8, String compressorType) {
			super(context, JSCompressorFactory.class.getName(), null);
			this.useV8 = useV8;
			this.compressorType = compressorType;
		}
		
		public Object addingService(ServiceReference reference) {
			String dojoServiceId = null;
			if (compressorType != null) {
				if (compressorType.equals("shrinksafe")) {
					dojoServiceId = "ShrinksafeJSCompressor";
				} else if (compressorType.equals("uglifyjs")) {
					if (useV8) {
						dojoServiceId = "V8UglifyJSCompressor";
					} else {
						dojoServiceId = "RhinoUglifyJSCompressor";
					}
				}
			}
			if (dojoServiceId != null && reference.getProperty("dojoServiceId").equals(dojoServiceId)) {
				jsCompressorFactory = (JSCompressorFactory)context.getService(reference);
				registerServlet();
				
			}
			return context.getService(reference);
		}
	}
	
	private class JSOptimizerFactoryServiceTracker extends ServiceTracker {
		private boolean useV8 = false;
		private String jsHandlerType = null;
		
		public JSOptimizerFactoryServiceTracker(BundleContext context, boolean useV8, String jsHandlerType) {
			super(context, JSOptimizerFactory.class.getName(), null);
			this.useV8 = useV8;
			this.jsHandlerType = jsHandlerType;
		}
		
		public Object addingService(ServiceReference reference) {
			String dojoServiceId = null;
			if (jsHandlerType.equals(JSHandler.AMD_HANDLER_TYPE)) {
				if (useV8) {
					dojoServiceId = "AMDV8JSOptimizer";
				} else {
					dojoServiceId = "AMDRhinoJSOptimizer";
				}
			} else {
				if (useV8) {
					dojoServiceId = "V8JSOptimizer";
				} else {
					dojoServiceId = "RhinoJSOptimizer";
				}
			}
			if (dojoServiceId != null && reference.getProperty("dojoServiceId").equals(dojoServiceId)) {
				jsOptimizerFactory = (JSOptimizerFactory)context.getService(reference);
				registerServlet();
			}
			return context.getService(reference);
		}
	}
	
	public class ResourceServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private ResourceLoader resourceLoader = null;
		
		public ResourceServlet(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			String target = request.getPathInfo();
			URL url = resourceLoader.getResource(target);
			if (url != null) {
				String mimeType = getServletContext().getMimeType(target);
				if (mimeType == null) {
					mimeType = "text/plain";
				}
				response.setContentType(mimeType);
				InputStream is = null;
				URLConnection urlConnection = null;
				ServletOutputStream os = response.getOutputStream();
				try {
					urlConnection = url.openConnection();
					long lastModifed = urlConnection.getLastModified();
					if (lastModifed > 0) {
					    String ifNoneMatch = request.getHeader("If-None-Match");
						
					    if (ifNoneMatch != null && ifNoneMatch.equals(Long.toString(lastModifed))) {
					    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					        return;
					    }

			 			response.setHeader("ETag", Long.toString(lastModifed));
					}
					is = urlConnection.getInputStream();
					byte[] buffer = new byte[4096];
					int len = 0;
					while((len = is.read(buffer)) != -1) {
						os.write(buffer, 0, len);
					}
				}
				finally {
					if (is != null) {try{is.close();}catch(IOException e){}}
				}
			}
			else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "path ["+target+"] not found");
			}
		}
	}
}
