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
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
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
	
	/**
	 * Default Constructor
	 * @param sExchange code of the exchange queue to use
	 */
	public Send(String sExchange) {
		
		if (sExchange == null) return;
		
		//create connection to the server
        try {
            m_oConnection = RabbitFactory.getConnectionFactory().newConnection();
            if (m_oConnection!=null) m_oChannel = m_oConnection.createChannel();
            m_sExchangeName = sExchange;
        } catch (Exception oEx) {
            WasdiLog.errorLog("Send.Init: Error connecting to rabbit " + oEx.toString());
        }
	}
	
	/**
	 * Free the resources
	 */
	public void Free() {
		try {
			
			if (m_oChannel != null) {
				m_oChannel.close();
			}
			
		} catch (IOException oEx) {
			WasdiLog.errorLog("Send.Free: Error closing channel " + oEx.toString());
		} catch (TimeoutException oEx) {
			WasdiLog.errorLog("Send.Free: Error closing channel " + oEx.toString());
		} catch (Exception oEx) {
			WasdiLog.errorLog("Send.Free: Error closing channel " + oEx.toString());
		}
		
		try {
			if (m_oConnection!=null) {
				m_oConnection.close();
			}
			
		} catch (IOException oEx) {
			WasdiLog.errorLog("Send.Free: Error closing connection " + oEx.toString());
		} catch (Exception oEx) {
			WasdiLog.errorLog("Send.Free: Error closing connection " + oEx.toString());
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
            
        } catch (IOException oEx) {
        	WasdiLog.errorLog("Send.SendMgs: Error sending message " + sMessageAttribute + " to " + sRoutingKey + " " + oEx.toString());
            return false;
        }
        catch (Exception oEx) {
        	WasdiLog.errorLog("Send.SendMgs: Error sending message " + sMessageAttribute + " to " + sRoutingKey + " " + oEx.toString());
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
        	WasdiLog.errorLog("Send.SendRabbitMessage: ERROR " + oEx.toString());
            return  false;
        }
    }
    
    /**
     * Send the redeploy done message for the processor
     * 
     * @param oParameter Processor Parameter
     * @param bSendToLocalNode True to send the message when the operation is done in the computing node. False to send it when is done on the main node
     * @return true if all ok false otherwise
     */
    public boolean sendRedeployDoneMessage(ProcessorParameter oParameter, boolean bSuccess, boolean bSendToLocalNode) {
    	
    	boolean bSendRet = false;
    	
        try {
        	// In the exchange we should have the workspace from there the user requested the Redeploy
        	String sOriginalWorkspaceId = oParameter.getExchange();
        	
        	// Check if it is valid
        	if (Utils.isNullOrEmpty(sOriginalWorkspaceId)==false) {
        		
        		// Read the workspace
        		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
        		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);
        		
        		if (oWorkspace != null) {
        			
    				// Prepare the message
		        	String sName = oParameter.getName();
		        	
		        	if (Utils.isNullOrEmpty(sName)) sName = "Your Processor";
		        	
		            String sInfo = "Re Deploy Done<br>" + sName + " is now available";
		            
		            if (!bSuccess) {
		            	sInfo = "GURU MEDITATION<br>There was an error re-deploying " + sName + " :(";
		            }
        			
        			String sNodeCode = "wasdi";
        			
        			if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
        				sNodeCode = oWorkspace.getNodeCode();
        			}	        			
        			
        			if (bSendToLocalNode) {
	        			// This is the computing node where the request came from?
	        			if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {
				            this.SendRabbitMessage(bSuccess, LauncherOperations.INFO.name(), oParameter.getExchange(), sInfo, oParameter.getExchange());
				            bSendRet = true;
	        			}	        				
        			}
        			else {
        				// This is the main node?
        				if (WasdiConfig.Current.isMainNode()) {
        					this.SendRabbitMessage(bSuccess, LauncherOperations.INFO.name(), oParameter.getExchange(), sInfo, oParameter.getExchange());
        					bSendRet = true;
        				}
        			}
        		}	        		
        	}
        	
        }
        catch (Exception oRabbitException) {
			WasdiLog.errorLog("Send.sendRedeployDoneMessage: exception sending Rabbit Message", oRabbitException);
		}
        
        return bSendRet;
    }    
}
