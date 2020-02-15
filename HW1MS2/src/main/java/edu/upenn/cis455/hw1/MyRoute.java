package edu.upenn.cis455.hw1;

//import org.apache.log4j.Logger;

import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Response;
import edu.upenn.cis455.hw1.interfaces.Route;

public class MyRoute {

//	static final Logger logger = Logger.getLogger(MyRoute.class);
	
	HttpMethod requestHttpMethod;
	String routePath;
	String acceptType;
	Route route;
	
	public MyRoute(HttpMethod requestHttpMethodArg, String routePathArg, String acceptTypeArg, Route routeArg) {
		requestHttpMethod = requestHttpMethodArg;
		routePath = routePathArg;
		acceptType = acceptTypeArg;
		route = routeArg;
	}
	
	public boolean match(HttpMethod httpMethod, Request request) {
		String requestPath = request.pathInfo();
		if(httpMethod == requestHttpMethod) {
			String[] pathSplit = requestPath.strip().split("/");
			String[] routePathSplit = routePath.split("/");
			if((pathSplit.length == routePathSplit.length) || (routePathSplit.length != 0 && (routePathSplit[routePathSplit.length - 1].equals("*") && pathSplit.length >= routePathSplit.length))) {
				for(int i = 0; i < pathSplit.length && i < routePathSplit.length; i++) {
					if(routePathSplit[i].length() == pathSplit[i].length() && pathSplit[i].length() == 0)continue;
					if(routePathSplit[i].charAt(0) == ':' || routePathSplit[i].equals("*") || routePathSplit[i].equals(pathSplit[i]))continue;
					return false;
				}
				return true;
			}
		}			
		return false;
	}
	
	public Object handle(Request request, Response response) throws Exception{
		return route.handle(request, response);
	}

	public String getRoutePath() {
		return routePath;
	}
}
