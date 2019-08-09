package it.fadeout.sftp;

import java.net.URI;
import java.util.concurrent.Semaphore;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import it.fadeout.Wasdi;

@ClientEndpoint
public class WsClient extends Semaphore {
	
	String m_sMessage;
	boolean ok;
	String data;
	private Session m_sUserSession;

	public WsClient(String sWsAddress, String sMessage) throws InterruptedException {
		super(0, true);
		m_sMessage = sMessage;
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		try {
			container.connectToServer(this, new URI(sWsAddress));
			acquire();
		} catch (Exception e) {
			e.printStackTrace();
			release();
		}		
	}
		
   @OnOpen
    public void onOpen(Session userSession) {
        Wasdi.debugLog("opening websocket...");
        m_sUserSession = userSession;
        Wasdi.debugLog("WsClient.onOpen: userSession = " + userSession.getId() );
        userSession.getAsyncRemote().sendText(m_sMessage);	        
   }
   
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
	    Wasdi.debugLog("closing websocket");		    
	}

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
    	
    	Wasdi.debugLog(message);
    	
    	ok = message.startsWith("OK;");
    	String[] toks = message.split(",|;");
		data = toks.length>1 ? toks[1] : "";
    	release();
    }

	public boolean isOk() {
		return ok;
	}

	public String getData() {
		return data;
	}
    
}
