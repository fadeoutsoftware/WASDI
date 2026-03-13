package it.fadeout;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class MiniWasdiServer {
    public static void main(String[] args) throws Exception {
        // 1. Initialize Jetty on the desired port
        int iPort = 8080;
        Server oServer = new Server(iPort);

        // 2. Set up the context handler (the "/" root)
        ServletContextHandler oContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        oContext.setContextPath("/");
        oServer.setHandler(oContext);

        // 3. Attach your existing 'Wasdi' ResourceConfig to the Jersey Servlet
        ServletHolder oJerseyServlet = new ServletHolder(new ServletContainer(new Wasdi()));
        oJerseyServlet.setInitOrder(0);
        
        oContext.setContextPath("/wasdiwebserver/rest");
        // This tells Jersey which path to listen on
        oContext.addServlet(oJerseyServlet, "/*");
        
        try {
            oServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }       
        
        try {
            oServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            oServer.destroy();
        }
    }
}