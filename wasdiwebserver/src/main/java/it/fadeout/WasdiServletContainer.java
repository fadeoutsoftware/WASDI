package it.fadeout;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.glassfish.jersey.servlet.ServletContainer;

public class WasdiServletContainer  extends ServletContainer {
    private final Wasdi wasdi;

    public WasdiServletContainer(Wasdi oWasdi) {
        super(oWasdi);
        this.wasdi = oWasdi;
    }

    @Override
    public void init(ServletConfig oConfig) throws ServletException {
        super.init(oConfig);
        wasdi.initWasdi();
    }    
}
