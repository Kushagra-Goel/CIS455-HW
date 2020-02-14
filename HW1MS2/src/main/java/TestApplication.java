import static edu.upenn.cis455.hw1.WebServiceController.*;

class TestApplication {
	public static void main(String args[]) {

    /* A simple static web page */

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
  }
}