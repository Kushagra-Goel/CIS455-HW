import static edu.upenn.cis455.hw1.WebServiceController.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;



class TestApplication {
	
    private static Map<String, Book> books = new HashMap<String, Book>();
    private static Map<String, String> usernamePasswords = new HashMap<String, String>();
    
	public static void main(String args[]) {

    /* A simple static web page */
		
	staticFileLocation("/home/cis455/Desktop/ServerFiles");
//	ipAddress("10.0.2.15");
	port(8080);
//	threadPool(1000);

    get("/", (request,response) -> {
    	return "Hello world!<p><a href=\"/login\">Go to the login page</a>";
    });

    /* Displays a login form if the user is not logged in yet (i.e., the "username" attribute
       in the session has not been set yet), and welcomes the user otherwise */

    get("/login", (request, response) -> {
      String name = (String)(request.session().attribute("username"));
      if (name == null) {
        return "<html><body>Please enter your user name: <form action=\"/checklogin\" method=\"POST\"><input type=\"text\" name=\"name\"/><input type=\"submit\" value=\"Log in\"/></form></body></html>";
      } else {
        return "<html><body>Hello, "+name+"!<p><a href=\"/logout\">Log out</a></body></html>";
      }
    });

    /* Receives the data from the login form, logs the user in, and redirects the user back to
       /login. Notice that, this being a POST request, the form data will be in the body of the
       request; see the link in the handout for more information about the format. */

    post("/checklogin", (request, response) -> {
      String name = request.queryParams("name");
      if (name != null) {
        request.session().attribute("username", name);
      }
      response.redirect("/login");
      return null;
    });

    /* Logs the user out by deleting the "username" attribute from the session. You could also
       invalidate the session here to get rid of the JSESSIONID cookie entirely. */

    get("/logout", (request, response) -> {
      request.session().removeAttribute("username");
      response.redirect("/");
      return null;
    });	
	
	
    get("/hello", (request, response) -> "Hello World!");

    post("/hello", (request, response) ->
        "Hello World: " + request.body()
    );

    get("/private", (request, response) -> {
        response.status(401);
        return "Go Away!!!";
    });

    get("/users/:name", (request, response) -> "Selected user: " + request.params(":name"));

    get("/news/:section", (request, response) -> {
        response.type("text/xml");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><news>" + request.params("section") + "</news>";
    });

    get("/protected", (request, response) -> {
        halt(403, "I don't think so!!!");
        return null;
    });

    get("/redirect", (request, response) -> {
        response.redirect("/news/world");
        return null;
    });

    get("/", (request, response) -> "root");
	
	
	

    get("/hi", (request, response) -> {
        request.attribute("foo", "bar");
        return null;
    });

    after("/hi", null ,(request, response) -> {
        for (String attr : request.attributes()) {
            System.out.println("attr: " + attr);
        }
    });

    after("/hi", null, (request, response) -> {
        Object foo = request.attribute("foo");
        response.type("");
        response.body(asXml("foo", foo));
    });

    usernamePasswords.put("foo", "bar");
    usernamePasswords.put("admin", "admin");

    before("/hello", null, (request, response) -> {
        String user = request.queryParams("user");
        String password = request.queryParams("password");

        String dbPassword = usernamePasswords.get(user);
        if (!(password != null && password.equals(dbPassword))) {
            halt(401, "You are not welcome here!!!");
        }
    });

    before("/hello", null, (request, response) -> response.header("Foo", "Set by second before filter"));

    get("/hello", (request, response) -> "Hello World!");

    after("/hello", null, (request, response) -> response.header("spark", "added by after-filter"));
	

    final Random random = new Random();

    // Creates a new book resource, will return the ID to the created resource
    // author and title are sent in the post body as x-www-urlencoded values e.g. author=Foo&title=Bar
    // you get them by using request.queryParams("valuename")
    post("/books", (request, response) -> {
        String author = request.queryParams("author");
        String title = request.queryParams("title");
        Book book = new Book(author, title);

        int id = random.nextInt(Integer.MAX_VALUE);
        books.put(String.valueOf(id), book);

        response.status(201); // 201 Created
        return id;
    });

    // Gets the book resource for the provided id
    get("/books/:id", (request, response) -> {
        Book book = books.get(request.params(":id"));
        if (book != null) {
            return "Title: " + book.getTitle() + ", Author: " + book.getAuthor();
        } else {
            response.status(404); // 404 Not found
            return "Book not found";
        }
    });

    // Updates the book resource for the provided id with new information
    // author and title are sent in the request body as x-www-urlencoded values e.g. author=Foo&title=Bar
    // you get them by using request.queryParams("valuename")
    put("/books/:id", (request, response) -> {
        String id = request.params(":id");
        Book book = books.get(id);
        if (book != null) {
            String newAuthor = request.queryParams("author");
            String newTitle = request.queryParams("title");
            if (newAuthor != null) {
                book.setAuthor(newAuthor);
            }
            if (newTitle != null) {
                book.setTitle(newTitle);
            }
            return "Book with id '" + id + "' updated";
        } else {
            response.status(404); // 404 Not found
            return "Book not found";
        }
    });

    // Deletes the book resource for the provided id
    delete("/books/:id", (request, response) -> {
        String id = request.params(":id");
        Book book = books.remove(id);
        if (book != null) {
            return "Book with id '" + id + "' deleted";
        } else {
            response.status(404); // 404 Not found
            return "Book not found";
        }
    });
    // Gets all available book resources (ids)
    get("/books", (request, response) -> {
        String ids = "";
        for (String id : books.keySet()) {
            ids += id + " ";
        }
        return ids;
    });

    
    get("/test/:only/single/*/steps", (request,response) -> {
    	return "Testing only single steps succeeded : " + request.params("only");
    });

    get("/test/:many/multiple/*/steps/*", (request,response) -> {
    	return "Testing many multiple steps succeeded : " + request.params("many");
    });

    
    get("/test/splat/:finalCount/multiple/*/steps/*/*/*/*", (request,response) -> {
    	return "<!DOCTYPE html><html><body>Testing multiple splats succeeded <br/><br/> ParamList : {finalCount :"+ request.params("finalCount") +"}<br/><br/> Splat List <ul><li>" + String.join("</li><li>", request.splat()) +"</li></ul></body></html>";
    });

    get("/test/splat/:finalCount/single/*/steps", (request,response) -> {
    	return "<!DOCTYPE html><html><body>Testing multiple splats succeeded <br/><br/> ParamList : {finalCount :"+ request.params("finalCount") +"}<br/><br/> Splat List <ul><li>" + String.join("</li><li>", request.splat()) +"</li></ul></body></html>";
    });

    get("/test/persistence", (request,response) -> {
    	response.header("Connection", "keep-alive");
    	return "<!DOCTYPE html><html><body><p>This connection should persist.</body></html>";
    });

    get("/test/chunking", (request,response) -> {
    	response.header("Transfer-Encoding", "chunked");
    	return "<!DOCTYPE html><html><body><p>This response was chunked.</body></html>";
    });
    
    post("/test/ChunkingInput", (request,response) -> {
    	Set<String> qp = request.queryParams();
    	String result = "<!DOCTYPE html>\n<html>\n<body>\n";
    	for(String key: qp) {
    		result = result + String.format("%s = %s<br>\n", key, request.queryParams(key));
    	}
		result = result + "</body>\n</html>\n";
    	return result;
    });
    
    get("/benchmark/:number", (request,response) -> {
    	long value = Long.valueOf(request.params("number"));
    	long count = 0;
    	for(long i = 2 ; i <= value; i++) {
    		boolean isPrime = true;
    		for(long j = 2; j <= value; j++) {
    			if( i == j)continue;
    			if(i%j == 0) {
    				isPrime = false;
    			}
    		}
    		if(isPrime)count++;
    	}
    	return "<!DOCTYPE html><html><body>There are " + String.valueOf(count) + " prime numbers upto " + String.valueOf(value) + "</body></html>";
    });
    get("/benchmark_sleep", (request,response) -> {
    	Thread.sleep(10);
    	return "<!DOCTYPE html><html><body>Thread is back from slumber</body></html>";
    });
    
    
  }
 private static String asXml(String name, Object value) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><" + name +">" + value + "</"+ name + ">";
    }


public static class Book {

    public String author, title;

    public Book(String author, String title) {
        this.author = author;
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
}