package wasdi.shared.rabbit;

/**
 * Created by s.adamo on 23/09/2016.
 */
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.RabbitMessageViewModel;

/**
 * Utility class to send Rabbit Messages
 * @author p.campanella
 *
 */
public class Send {
	
	Connection m_oConnection = null;
	Channel m_oChannel= null;
	String m_sExchangeName = "amq.topic";
	
	public Send(String sExchange) {
		
		if (sExchange == null) return;
		
		//create connection to the server
        try {
            m_oConnection = RabbitFactory.getConnectionFactory().newConnection();
            if (m_oConnection!=null) m_oChannel = m_oConnection.createChannel();
            m_sExchangeName = sExchange;
        } catch (Exception e) {
            WasdiLog.debugLog("Send.Init: Error connecting to rabbit " + e.toString());
        }
	}
	
	public void Free() {
		try {
			
			if (m_oChannel != null) {
				m_oChannel.close();
			}
			
			if (m_oConnection!=null) {
				m_oConnection.close();
			}
			
		} catch (IOException e) {
			WasdiLog.debugLog("Send.Free: Error closing connection " + e.toString());
		} catch (TimeoutException e) {
			WasdiLog.debugLog("Send.Free: Error closing connection " + e.toString());
		}
	}
	
    /**
     * Send a Rabbit Message
     * @param sRoutingKey (Is the workspace Id)
     * @param sMessageAttribute
     * @return true if the message is sent, false otherwise
     * @throws IOException
     */
    private boolean SendMsg(String sRoutingKey, String sMessageAttribute)
    {
    	if (m_oConnection == null || m_oChannel == null) {
    		WasdiLog.debugLog("Send.SendMgs: impossibile to send " + sMessageAttribute + " to " + sRoutingKey);
    		return false;
    	}
    	
        try {        	
            m_oChannel.basicPublish(m_sExchangeName, sRoutingKey, null, sMessageAttribute.getBytes());
            return true;
            
        } catch (IOException e) {
        	WasdiLog.debugLog("Send.SendMgs: Error sending message " + sMessageAttribute + " to " + sRoutingKey + " " + e.toString());
            return false;
        }
        catch (Exception e) {
        	WasdiLog.debugLog("Send.SendMgs: Error sending message " + sMessageAttribute + " to " + sRoutingKey + " " + e.toString());
            return false;
        }
    }

    /**
     * Sends Update Process Rabbit Message
     * @param oProcess
     * @return
     * @throws JsonProcessingException
     */
    public boolean SendUpdateProcessMessage(ProcessWorkspace oProcess) throws JsonProcessingException {  
    	
    	if (oProcess==null) return false;
    	if (m_oConnection == null) return false;
    	if (m_oChannel == null) return false;
    	
        RabbitMessageViewModel oUpdateProcessMessage = new RabbitMessageViewModel();
        oUpdateProcessMessage.setMessageCode(LauncherOperations.UPDATEPROCESSES.name());
        oUpdateProcessMessage.setWorkspaceId(oProcess.getWorkspaceId());
        oUpdateProcessMessage.setPayload(oProcess.getProcessObjId() + ";" + oProcess.getStatus() + ";" + oProcess.getProgressPerc());
        
        if(0 == oProcess.getProgressPerc() % 10) {
        	WasdiLog.debugLog("Send.SendUpdateProcessMessage: Send update message for process " + oProcess.getProcessObjId() + ": " + oUpdateProcessMessage.getPayload());
        }
        
        String sJSON = MongoRepository.s_oMapper.writeValueAsString(oUpdateProcessMessage);
        return SendMsg(oProcess.getWorkspaceId(), sJSON);
    }

    /**
     * Send a Generic WASDI Rabbit Message
     * @param bOk Flag ok or not
     * @param sMessageCode Message Code
     * @param sWorkSpaceId Reference Workspace
     * @param oPayload Message Payload
     * @param sExchangeId Exchange Routing Key
     * @return
     */
    public boolean SendRabbitMessage(boolean bOk, String sMessageCode, String sWorkSpaceId, Object oPayload, String sExchangeId) {
    	
    	if (m_oConnection == null) return false;
    	if (m_oChannel == null) return false;

        try {
            RabbitMessageViewModel oRabbitVM = new RabbitMessageViewModel();
            oRabbitVM.setMessageCode(sMessageCode);
            oRabbitVM.setWorkspaceId(sWorkSpaceId);
            if (bOk) oRabbitVM.setMessageResult("OK");
            else  oRabbitVM.setMessageResult("KO");

            oRabbitVM.setPayload(oPayload);

            String sJSON = MongoRepository.s_oMapper.writeValueAsString(oRabbitVM);

            return SendMsg(sExchangeId, sJSON);
        }
        catch (Exception oEx) {
        	WasdiLog.debugLog("Send.SendRabbitMessage: ERROR " + oEx.toString());
            return  false;
        }

    }
}
