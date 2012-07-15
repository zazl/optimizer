/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.json.JSONSerializer;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

public class RhinoASTScriptAnalyzer extends ScriptAnalyzer implements NodeVisitor {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.servlet");
	
	private Map<String, Object> results = null;

	public RhinoASTScriptAnalyzer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader) {
		super(resourceLoader, rhinoClassLoader);
	}
	
	public Map<String, Object> analyze(String script) throws IOException {
		results = new HashMap<String, Object>();
		results.put("dependencies", Collections.EMPTY_LIST);
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        Parser parser = new Parser(compilerEnv, compilerEnv.getErrorReporter());
		
		try {
			AstRoot ast = parser.parse(script, null, 1);
            ast.visit(this);
		} catch (EvaluatorException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "analyze", "EvaluatorException while parsing script", e);
			throw new IOException("EvaluatorException while parsing script", e);
		}
		StringWriter sw = new StringWriter();
		JSONSerializer.serialize(sw, results.get("dependencies"), true);
		logger.logp(Level.FINE, getClass().getName(), "analyze", "dependencies : "+sw.toString());
		if (results.get("config") != null) {
			sw = new StringWriter();
			JSONSerializer.serialize(sw, results.get("config"), true);
			logger.logp(Level.FINE, getClass().getName(), "analyze", "config : "+sw.toString());
		}
		
		return results;
	}

	public boolean visit(AstNode astNode) {
		if (astNode instanceof VariableDeclaration) {
			VariableDeclaration vd = (VariableDeclaration)astNode;
			for (VariableInitializer vi : vd.getVariables()) {
				if (vi.getTarget().getType() == Token.NAME) {
					Name name = (Name)vi.getTarget();
					if (name.getIdentifier().equals("zazlConfig") && vi.getInitializer() instanceof ObjectLiteral) {
						results.put("config", parseObject((ObjectLiteral)vi.getInitializer()));
					}
				}
			}
		} else if (astNode instanceof FunctionCall) {
			FunctionCall functionCall = (FunctionCall)astNode;
			AstNode target = functionCall.getTarget();
			String callName = "";
			if (target instanceof Name) {
				callName = ((Name)target).getIdentifier();
			}
			if (callName.equals("zazl") || callName.equals("require")) {
				List<AstNode> args = functionCall.getArguments();
				if (args.get(0) instanceof ArrayLiteral) {
					results.put("dependencies", getDependencies((ArrayLiteral)args.get(0)));
				} else if (args.get(0) instanceof ObjectLiteral && args.get(1) instanceof ArrayLiteral) {
					results.put("config", parseObject((ObjectLiteral)args.get(0)));
					results.put("dependencies", getDependencies((ArrayLiteral)args.get(1)));
				}
			}
		}
		return true;
	}
	
	private static List<String> getDependencies(ArrayLiteral al) {
		List<String> deps = new ArrayList<String>();
		for (AstNode dependency : al.getElements()) {
			if (dependency instanceof StringLiteral) {
				deps.add(((StringLiteral)dependency).getValue());
			}
		}
		return deps;
	}
	
	private static Map<String, Object> parseObject(ObjectLiteral ol) {
		Map<String, Object> obj = new HashMap<String, Object>();
		for (ObjectProperty op : ol.getElements()) {
			String name = op.getLeft().toSource();
			AstNode right = op.getRight();
			int type = right.getType();
			switch (type) {
				case Token.ARRAYLIT:
					obj.put(name, parseArray((ArrayLiteral)right));
					break;
				case Token.OBJECTLIT: 
					obj.put(name, parseObject((ObjectLiteral)right));
					break;
				case Token.NAME: 
					obj.put(name, ((Name)right).getIdentifier());
					break;
				case Token.NUMBER: 
					obj.put(name, ((NumberLiteral)right).getNumber());
					break;
				case Token.STRING: 
					obj.put(name, ((StringLiteral)right).getValue());
					break;
				case Token.TRUE: 
					obj.put(name, Boolean.TRUE);
					break;
				case Token.FALSE: 
					obj.put(name, Boolean.FALSE);
					break;
			}
		}
		return obj;
	}
	
	private static List<Object> parseArray(ArrayLiteral al) {
		List<Object> array = new ArrayList<Object>();
		for (AstNode an : al.getElements()) {
			int type = an.getType();
			switch (type) {
				case Token.ARRAYLIT:
					array.add(parseArray((ArrayLiteral)an));
					break;
				case Token.OBJECTLIT: 
					array.add(parseObject((ObjectLiteral)an));
					break;
				case Token.NAME: 
					array.add(((Name)an).getIdentifier());
					break;
				case Token.NUMBER: 
					array.add(((NumberLiteral)an).getNumber());
					break;
				case Token.STRING: 
					array.add(((StringLiteral)an).getValue());
					break;
			}
		}
		return array;
	}
}
