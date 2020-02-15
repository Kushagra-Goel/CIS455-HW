package edu.upenn.cis455.frontend;

import static spark.Spark.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.sleepycat.bind.tuple.StringBinding;

import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.ChannelStore;
import edu.upenn.cis455.storage.CrawledDocument;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.DocStore;
import edu.upenn.cis455.storage.User;
import edu.upenn.cis455.storage.UserStore;


class XPathApp {
	static final Logger logger = Logger.getLogger(XPathApp.class);
	

    private static UserStore userStore;
    private static DocStore docStore;
    private static ChannelStore channelStore;
    
    
	public static void main(String args[]) {

    if (args.length != 1) {
      System.err.println("You need to provide the path to the BerkeleyDB data store!");
      System.exit(1);
    }
    
    DBWrapper db = new DBWrapper(args[0]);
    
    userStore = new UserStore(db);
    docStore = new DocStore(db);
    channelStore = new ChannelStore(db);
    
  
    port(8080);

    get("/", (request,response) -> {
    	if(request.session(false) == null) {
        	return "<html><head></head><body>Please enter your username and password: <form action=\"/login\" method=\"POST\">\n" + 
        			"    <input type=\"text\" name=\"username\" placeholder=\"Username\"><br>\n" + 
        			"    <input type=\"password\" name=\"password\" placeholder=\"Password\"><br>\n" + 
        			"    <input type=\"submit\" value=\"Log in\">\n" + 
        			"</form></body></html>" + 
        			"<a href=\"/newaccount\">New Account Page</a>";    		
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<body>");
        sb.append(String.format("<h2>Welcome, %s %s</h2><br>", user.getFirstName(), user.getLastName()));
        sb.append("<a href=\"/logout\">Logout</a><br>");
        ArrayList<Channel> channels = channelStore.getAllChannels();
        sb.append("<ol>");
        
        for(Channel channel: channels) {
    		if(user.getChannelNames().contains(channel.getName())) {
            	sb.append(String.format("<li><a href=\"/show?name=%s\">%s</a> </li>", channel.getName(), channel.getName())); 
        		sb.append("<ul>");
            	sb.append(String.format("<li><a href=\"/unsubscribe?name=%s\">unsubscribe</a> </li>", channel.getName())); 
        	}else {
            	sb.append(String.format("<li> %s </li>", channel.getName()));
        		sb.append("<ul>");
            	sb.append(String.format("<li><a href=\"/subscribe?name=%s\">subscribe</a> </li>", channel.getName())); 
        	}
			if(channel.getOwnerUserName().equals(user.getUserName())) { 
            	sb.append(String.format("<li><a href=\"/delete?name=%s\">delete</a> </li>", channel.getName())); 
    		}
    		sb.append("</ul>");
        }
        
        sb.append("</ol>");
        
        sb.append("<h2>Create Channel:</h2>");
    	sb.append("<form action=\"/create\" method=\"GET\">\n" + 
    			"    <input type=\"text\" name=\"name\" placeholder=\"Channel Name\"><br>\n" + 
    			"    <input type=\"text\" name=\"xpath\" placeholder=\"XPath\"><br>\n" + 
    			"    <input type=\"submit\" value=\"Submit\">\n" + 
    			"</form>");  
        
        
        sb.append("</body></html>");
        response.type("text/html");
        response.body(sb.toString());
        return response.body();
        
    });
	
    get("/newaccount", (request,response) -> {
    	return "<html><head></head><body>Please enter your firstname, lastname, username and password: <form action=\"/register\" method=\"POST\">\n" + 
    			"    <input type=\"text\" name=\"firstname\" placeholder=\"First Name\"><br>\n" + 
    			"    <input type=\"text\" name=\"lastname\" placeholder=\"Last Name\"><br>\n" + 
    			"    <input type=\"text\" name=\"username\" placeholder=\"Username\"><br>\n" + 
    			"    <input type=\"password\" name=\"password\" placeholder=\"Password\"><br>\n" + 
    			"    <input type=\"submit\" value=\"Register\">\n" + 
    			"</form></body></html>";
    });
    
    post("/register", (request, response) -> {
    	try {
	    	User user = userStore.getUserInfo(request.queryParams("username"));
	    	if(user != null) {
	    		return "<html><head></head><body>Error : The user already exists.</body></html>";    		
	    	}
	    	userStore.addUserInfo(new User(request.queryParams("firstname"), request.queryParams("lastname"), request.queryParams("username"), request.queryParams("password"), new ArrayList<String>()));
	    	response.redirect("/");
//	    	logger.debug("registered : " + String.join("," , request.queryParams("firstname"), request.queryParams("lastname"), request.queryParams("username"), request.queryParams("password")));
		} catch(Exception e) {
			e.printStackTrace();
		}
    	return null;
      });
    
    
    post("/login", (request, response) -> {
    	try {
	    	User user = userStore.getUserInfo(request.queryParams("username"));
	    	logger.debug("Login Request : " + String.join("," , request.queryParams("username"),request.queryParams("password")));
	    	if(user == null) {
	    		return "<html><head></head><body>Error : Incorrect Credentials</body></html>";    		
	    	}
	    	
	    	
	    	//SHA 256 encrypted passwords only
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    	if(!Arrays.equals(user.getPasswordHash(),digest.digest(request.queryParams("password").getBytes(StandardCharsets.UTF_8)))) {
	    		return "<html><head></head><body>Error : Incorrect Credentials</body></html>";     		
	    	}
	    	request.session().attribute("username", user.getUserName());
	    	response.redirect("/");
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    });


    get("/lookup", (request,response) -> {
    	CrawledDocument doc = docStore.getDocument(new URLInfo(request.queryParams("url")));
    	if(doc == null) {
        	logger.info("Doc Not Found");
    		halt(404);
    	}
    	response.status(200);
    	response.type(doc.getDocType());
        return doc.getDoc();
    });
 
    get("/logout", (request, response) -> {
        request.session().invalidate();
      	response.redirect("/");
      	return null;
      });
    

    
    
    get("/create", (request, response) -> {
    	if(request.session(false) == null) {
    		halt(401, "Error 401 : No user is logged in");	
    		return null;
    	}
    	Channel channel = channelStore.getChannel(request.queryParams("name"));
    	if(channel != null) {
    		halt(409, "Error 409 : Channel with specified name already exists");
    		return null;
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
    	
    	channel = new Channel(request.queryParams("name"), request.queryParams("xpath"), user.getUserName(), new ArrayList<URLInfo>());
    	channelStore.addChannel(channel);
    	response.type("text/html");
    	return "<html><body> Successfully created channel </body></html>";
    });

    
    
    get("/delete", (request, response) -> {
    	if(request.session(false) == null) {
    		halt(401, "Error 401 : No user is logged in");	
    		return null;
    	}
    	Channel channel = channelStore.getChannel(request.queryParams("name"));
    	if(channel == null) {
    		halt(404, "Error 404 : No channel with specified name exists");
    		return null;
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
    	if(!channel.getOwnerUserName().equals(user.getUserName())) {
    		halt(403, "Error 403 : User is not the owner of the channel");
    		return null;
    	}
    	
    	for(User u: userStore.getAllUsers()) {
    		if(u.getChannelNames().contains(channel.getName())) {
    			u.getChannelNames().remove(channel.getName());
    			userStore.addUserInfo(u);
    		}
    	}
    	channelStore.deleteChannel(channel);

    	response.type("text/html");
    	return "<html><body> Successfully deleted channel </body></html>";
    });


    
    get("/show", (request, response) -> {
    	if(request.session(false) == null) {
    		halt(401, "Error 401 : No user is logged in");	
    	}    	
    	Channel channel = channelStore.getChannel(request.queryParams("name"));
    	if(channel == null) {
    		halt(404, "Error 404 : No channel with specified name exists");
    		return null;
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
    	if(!user.getChannelNames().contains(channel.getName())) {
    		halt(404, "Error 404 : Not subscribed to the channel");
    		return null;
    	}
    	

    	User owner = userStore.getUserInfo(channel.getOwnerUserName());
    	
    	StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<body>");
        sb.append("<div class=\"channelheader\">");
        sb.append(String.format("<h3>Channel Name: %s, created by: %s %s</h3>", channel.getName(), owner.getFirstName(), owner.getLastName()));
    	sb.append("</div>");
    	
    	for(URLInfo url :channel.getDocUrls()) {
    		CrawledDocument doc = docStore.getDocument(url);
    		String time = doc.getCrawlTimestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    		sb.append(String.format("Crawled on: %s, ", time));
    		sb.append(String.format("Location: %s", url.getFullURL()));
    		sb.append("<p>");
    		sb.append("<div class=\"document\">");
    		sb.append(doc.getDoc());
    		sb.append("</div>");
    		sb.append("</p>");
    	}

        sb.append("</body></html>");
        response.type("text/html");
        response.body(sb.toString());
        return response.body();
    });
    
    
    
    get("/subscribe", (request, response) -> {
    	if(request.session(false) == null) {
    		halt(401, "Error 401 : No user is logged in");	
    		return null;
    	}
    	Channel channel = channelStore.getChannel(request.queryParams("name"));
    	if(channel == null) {
    		halt(404, "Error 404 : No channel with specified name exists");
    		return null;
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
    	if(user.getChannelNames().contains(channel.getName())) {
    		halt(409, "Error 409 : Already subscribed to the channel");
    		return null;
    	}
    	
    	user.getChannelNames().add(channel.getName());
    	userStore.addUserInfo(user);

    	response.type("text/html");
    	return "<html><body> Successfully subscribed to channel </body></html>";
    });

    
    
    get("/unsubscribe", (request, response) -> {
    	if(request.session(false) == null) {
    		halt(401, "Error 401 : No user is logged in");	
    		return null;
    	}
    	Channel channel = channelStore.getChannel(request.queryParams("name"));
    	if(channel == null) {
    		halt(404, "Error 404 : No channel with specified name exists");
    		return null;
    	}
    	User user = userStore.getUserInfo(request.session().attribute("username"));
    	if(!user.getChannelNames().contains(channel.getName())) {
    		halt(404, "Not subscribed to the channel");
    		return null;
    	}

    	user.getChannelNames().remove(channel.getName());
    	userStore.addUserInfo(user);

    	response.type("text/html");
    	return "<html><body> Successfully unsubscribed from channel </body></html>";
    });
    

    
    get("/shutdown", (request, response) -> {
    	stop();
    	if(docStore != null)docStore.close();
    	return null;
    });
    
  }
}