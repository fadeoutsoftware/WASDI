package it.fadeout;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.glassfish.jersey.servlet.ServletContainer;

public class WasdiServletContainer  extends ServletContainer {
    private final Wasdi wasdi;

    public WasdiServletContainer(Wasdi wasdi) {
        super(wasdi);
        this.wasdi = wasdi;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        wasdi.initWasdi();
    }    
}
