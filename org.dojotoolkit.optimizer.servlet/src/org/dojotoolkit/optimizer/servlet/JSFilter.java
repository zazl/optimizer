/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.dojotoolkit.optimizer.JSOptimizer;

public class JSFilter implements Filter {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.servlet");
	
	private ResourceLoader resourceLoader = null;
	private RhinoClassLoader rhinoClassLoader = null;
	private JSOptimizer jsOptimizer = null;
	private FilterConfig filterConfig = null;
	
	public JSFilter() {};
	
	public JSFilter(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader) {
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		PrintWriter out = response.getWriter();
		String requestURI = ((HttpServletRequest)request).getRequestURI();
		String path = "";
		logger.logp(Level.FINE, getClass().getName(), "doFilter", "pathInfo ["+requestURI+"]");
		
		for (Enumeration<String> e = ((HttpServletRequest)request).getHeaderNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			String headerValue = ((HttpServletRequest)request).getHeader(headerName);
			logger.logp(Level.FINEST, getClass().getName(), "doFilter", "header : "+headerName+" : "+headerValue);
		}
		
		if (requestURI.lastIndexOf('/') != -1) {
			path = requestURI.substring(0, requestURI.lastIndexOf('/')+1);
		}
		RequestWrapper requestWrapper = new RequestWrapper((HttpServletRequest)request);
		ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse)response);
		chain.doFilter(requestWrapper, responseWrapper);
		if (resourceLoader == null) {
			resourceLoader = (ResourceLoader)filterConfig.getServletContext().getAttribute("org.dojotoolkit.ResourceLoader");
		}
		if (rhinoClassLoader == null) {
			rhinoClassLoader = (RhinoClassLoader)filterConfig.getServletContext().getAttribute("org.dojotoolkit.RhinoClassLoader");
		}
		if (jsOptimizer == null) {
			jsOptimizer = (JSOptimizer)filterConfig.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		}
		CharArrayWriter caw = new CharArrayWriter();
		String responseText = responseWrapper.toString();
		if (responseText != null && responseText.length() > 0) {
			logger.logp(Level.FINE, getClass().getName(), "doFilter", "parsing html for "+requestURI);
			HTMLParser parser = new HTMLParser(caw, 
					                           response.getCharacterEncoding(), 
					                           resourceLoader, 
					                           rhinoClassLoader, 
					                           jsOptimizer, 
					                           request.getLocale(), 
					                           ((HttpServletRequest)request).getContextPath(),
					                           path);
			parser.parse(responseText);
			String result = caw.toString();
			response.setContentLength(result.length());
			out.write(result);
			out.close();
		}
	}

	public void destroy() {
		filterConfig = null;
	}
	
	public class ServletOutputStreamWrapper extends ServletOutputStream {
		ByteArrayOutputStream stream = null;
		
		public ServletOutputStreamWrapper() {
			stream = new ByteArrayOutputStream(1024 * 64);
		}
		
		public void write(int b) throws IOException {
			stream.write(b);
		}
		
		public String toString(String encoding) {
			try {
				return stream.toString(encoding);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public class ResponseWrapper extends HttpServletResponseWrapper {
		private CharArrayWriter writer = null;
		private PrintWriter pw = null;
		private ServletOutputStreamWrapper stream = null;
		private int status;

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		public PrintWriter getWriter() {
			if (stream != null) {
	            throw new IllegalStateException("Cannot call getWriter() after getOutputStream()");
	        }
			
			if (writer == null) {
				writer = new CharArrayWriter();
				pw = new PrintWriter(writer); 
			}
			return pw;
		}
		
		public ServletOutputStream getOutputStream() {
			if (writer != null) {
	            throw new IllegalStateException("Cannot call getOutputStream() after getWriter()");
	        }
			if (stream == null) {
				stream = new ServletOutputStreamWrapper();
			}
			return stream;
		}
		
		public String toString() {
			if (writer != null) {
				return writer.toString();
			} else if (stream != null) {
				return stream.toString(getResponse().getCharacterEncoding());
			} else {
				return null;
			}
		}
		
		public void	setStatus(int sc) {
			super.setStatus(sc);
			status = sc;
		}
		
		public void	setStatus(int sc, String sm) {
			super.setStatus(sc, sm);
			status = sc;
		}
		
		public int getStatus() {
			return status;
		}
	}
	
	public class RequestWrapper extends HttpServletRequestWrapper {

		public RequestWrapper(HttpServletRequest request) {
			super(request);
		}

		public String getHeader(String name) {
			logger.logp(Level.FINE, getClass().getName(), "getHeader", "getting header with name ["+name+"]");
			if (name.equalsIgnoreCase("If-None-Match") || name.equalsIgnoreCase("If-Modified-Since") || name.equalsIgnoreCase("If-Match")) {
				logger.logp(Level.FINE, getClass().getName(), "getHeader", "Skipping adding header ["+name+"]");
				return null;
			} else {
				return super.getHeader(name);
			}
		}
		
		public Enumeration getHeaderNames() {
			HttpServletRequest request = (HttpServletRequest)getRequest();
			List<String> headerNamelist = new ArrayList<String>();
			for (Enumeration<String> e = ((HttpServletRequest)request).getHeaderNames(); e.hasMoreElements();) {
				String headerName = e.nextElement();
				if (!headerName.equalsIgnoreCase("If-None-Match") && !headerName.equalsIgnoreCase("If-Modified-Since") && !headerName.equalsIgnoreCase("If-Match")) {
					headerNamelist.add(headerName);
				} else {
					logger.logp(Level.FINE, getClass().getName(), "getHeader", "Skipping adding header ["+headerName+"]");
				}
			}
			
			return Collections.enumeration(headerNamelist);
		}
	}
}
