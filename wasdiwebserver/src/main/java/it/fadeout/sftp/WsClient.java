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

import wasdi.shared.utils.log.WasdiLog;

/**
 * Web Socket client. Used to talk with the mini web-socket server
 * installed in each server to handle the sftp credentials of the users.
 * This class is used only SFTPManager
 * @author p.campanella
 *
 */
@ClientEndpoint
public class WsClient extends Semaphore {
	

	private static final long serialVersionUID = 1L;
	
	/**
	 * Message to send
	 */
	String m_sMessage;
	/**
	 * True if the command succedeed, false otherwise
	 */
	boolean m_bOk;
	/**
	 * Complete server response
	 */
	String m_sData;
	
	/**
	 * Create a new Web Socket client
	 * @param sWsAddress Address of the local server
	 * @param sMessage Message to send
	 * @throws InterruptedException
	 */
	public WsClient(String sWsAddress, String sMessage) throws InterruptedException {
		super(0, true);
		m_sMessage = sMessage;
		WebSocketContainer oContainer = ContainerProvider.getWebSocketContainer();
		try {
			oContainer.connectToServer(this, new URI(sWsAddress));
			acquire();
		} catch (Exception e) {
			e.printStackTrace();
			release();
		}		
	}
	
	/**
	 * Callback when the communication is open: sends the message
	 * @param oUserSession
	 */
   @OnOpen
    public void onOpen(Session oUserSession) {
	   
        WasdiLog.debugLog("opening websocket...");
        WasdiLog.debugLog("WsClient.onOpen: userSession = " + oUserSession.getId() );
        // Send the message
        oUserSession.getAsyncRemote().sendText(m_sMessage);	        
   }
   
   /**
    * Callback when the connection is closed
    * @param oUserSession
    * @param oReason
    */
	@OnClose
	public void onClose(Session oUserSession, CloseReason oReason) {
	    WasdiLog.debugLog("closing websocket");		    
	}

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param sMessage The text message
     */
    @OnMessage
    public void onMessage(String sMessage) {
    	
    	WasdiLog.debugLog(sMessage);
    	
    	m_bOk = sMessage.startsWith("OK;");
    	String[] toks = sMessage.split(",|;");
		m_sData = toks.length>1 ? toks[1] : "";
    	release();
    }
    
    /**
     * Returns the status of the communication
     * @return
     */
	public boolean isOk() {
		return m_bOk;
	}
	
	/**
	 * Obtain the data received from the server
	 * @return
	 */
	public String getData() {
		return m_sData;
	}
    
}
