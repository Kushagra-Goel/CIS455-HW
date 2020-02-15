package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Response;
import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyFilter {

	String filterPath, acceptType;
	Filter filter;
	
	public MyFilter(String filterPathArg, String acceptTypeArg, Filter fiterArg) {
		filterPath = filterPathArg;
		acceptType = acceptTypeArg;
		filter = fiterArg;
	}
	
	public boolean match(Request request) {
		
		if(acceptType != null) {
			String acceptHeader = request.headers("accept");
			if(acceptHeader != null) {
				boolean foundMatch = false;
				for(String acceptedMime: acceptHeader.strip().split(",")) {
					for(String acceptedMimeCleaned : acceptedMime.split(";")) {
						if(acceptedMimeCleaned.contains("*")) {
							String[] splitAcceptedMime = acceptedMimeCleaned.split("/");
							String[] splitAcceptedFilter = acceptType.split("/");
							if((splitAcceptedMime.length == splitAcceptedFilter.length) || (splitAcceptedMime.length != 0 && (splitAcceptedMime[splitAcceptedMime.length - 1].equals("*") && splitAcceptedFilter.length >= splitAcceptedMime.length))) {
								boolean splitFoundMatch = true;
								for(int i = 0; i < splitAcceptedMime.length && i < splitAcceptedFilter.length; i++) {
									if(splitAcceptedMime[i].length() == splitAcceptedFilter[i].length() && splitAcceptedFilter[i].length() == 0)continue;
									if(splitAcceptedMime[i].charAt(0) == ':' || splitAcceptedMime[i].equals("*") || splitAcceptedMime[i].equals(splitAcceptedFilter[i]))continue;
									splitFoundMatch = false;
								}
								if(splitFoundMatch) {
									foundMatch = true;
									break;
								}
							}
						}
						else {
							if(acceptedMimeCleaned.equals(acceptType)) {
								foundMatch = true;
								break;
							}
						}
					}
					if(foundMatch)break;
				}
				if(!foundMatch) {
					return false;
				}
			}
		}
		
		if(filterPath != null) {
			String requestPath = request.pathInfo();
			String[] pathSplit = requestPath.strip().split("/");
			String[] filterPathSplit = filterPath.split("/");
			if((pathSplit.length == filterPathSplit.length) || (filterPathSplit.length != 0 && (filterPathSplit[filterPathSplit.length - 1].equals("*") && pathSplit.length >= filterPathSplit.length))) {
				for(int i = 0; i < pathSplit.length && i < filterPathSplit.length; i++) {
					if(filterPathSplit[i].length() == pathSplit[i].length() && pathSplit[i].length() == 0)continue;
					if(filterPathSplit[i].charAt(0) == ':' || filterPathSplit[i].equals("*") || filterPathSplit[i].equals(pathSplit[i]))continue;
					return false;
				}
				return true;
			}
			return false;	
		}
		return true;
	}
	
	public void handle(Request request, Response response) throws Exception{
		filter.handle(request, response);
	}

}
