package it.fadeout;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import wasdi.shared.utils.log.WasdiLog;

public class MiniWasdiServer {
    public static void main(String[] args) throws Exception {
        //  Initialize Jetty on the desired port
        int iPort = 8080;
        Server oServer = new Server(iPort);

        // Set up the context handler (the "/" root)
        ServletContextHandler oContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        oContext.setContextPath("/");
        oServer.setHandler(oContext);

        Wasdi oWasdi = new Wasdi();

        // Attach 'Wasdi' ResourceConfig to the Jersey Servlet
        ServletHolder oJerseyServlet = new ServletHolder(new WasdiServletContainer(oWasdi));
        oJerseyServlet.setInitOrder(0);
        
        oContext.setContextPath("/wasdiwebserver/rest");
        // This tells Jersey which path to listen on
        oContext.addServlet(oJerseyServlet, "/*");
        
        try {
        	// Lets go!
            oServer.start();
        } catch (Exception oEx) {
            WasdiLog.errorLog("MiniWasdiServer.main: start exception ", oEx);
        }       
        
        try {
            oServer.join();
        } catch (Exception oEx) {
        	WasdiLog.errorLog("MiniWasdiServer.main: join exception ", oEx);
        } finally {
            oServer.destroy();
        }
    }
}