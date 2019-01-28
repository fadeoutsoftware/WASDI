package wasdi.shared.rabbit;

/**
 * Created by s.adamo on 23/09/2016.
 */
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.viewmodels.RabbitMessageViewModel;

/**
 * Utility class to send Rabbit Messages
 * @author p.campanella
 *
 */
public class Send {
	
	Connection oConnection = null;
	Channel oChannel= null;
	String m_sExchangeName = "amq.topic";
	
	public Send(String sExchange) {
		//create connection to the server
        try {
            oConnection = RabbitFactory.getConnectionFactory().newConnection();
            if (oConnection!=null) oChannel = oConnection.createChannel();
            m_sExchangeName = sExchange;
        } catch (Exception e) {
            System.out.println("Send.Init: Error connecting to rabbit " + e.toString());
        }
	}
	
	public void Free() {
		try {
			if (oConnection!=null) oConnection.close();
		} catch (IOException e) {
			System.out.println("Send.Free: Error closing connection " + e.toString());
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
    	if (oChannel == null) return false;
    	
        try {        	
            oChannel.basicPublish(m_sExchangeName, sRoutingKey, null, sMessageAttribute.getBytes());
        } catch (IOException e) {
        	System.out.println("Send.SendMgs: Error publishing message " + sMessageAttribute + " to " + sRoutingKey + " " + e.toString());
            return false;
        }
        //LauncherMain.s_oLogger.debug(" [x] Sent '" + sMessageAttribute + "' to " + sRoutingKey);
        return true;

    }

    /**
     * Sends Update Process Rabbit Message
     * @param oProcess
     * @return
     * @throws JsonProcessingException
     */
    public boolean SendUpdateProcessMessage(ProcessWorkspace oProcess) throws JsonProcessingException {  
    	
    	if (oProcess==null) return false;
    	
        RabbitMessageViewModel oUpdateProcessMessage = new RabbitMessageViewModel();
        oUpdateProcessMessage.setMessageCode(LauncherOperations.UPDATEPROCESSES.name());
        oUpdateProcessMessage.setWorkspaceId(oProcess.getWorkspaceId());
        oUpdateProcessMessage.setPayload(oProcess.getProcessObjId() + ";" + oProcess.getStatus() + ";" + oProcess.getProgressPerc());
        
        System.out.println("Send.SendUpdateProcessMessage: Send update message for process " + oProcess.getProcessObjId() + ": " + oUpdateProcessMessage.getPayload());
        
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
        	System.out.println("Send.SendRabbitMessage: ERROR " + oEx.toString());
            return  false;
        }

    }
}
