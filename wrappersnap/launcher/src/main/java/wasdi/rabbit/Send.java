package wasdi.rabbit;

/**
 * Created by s.adamo on 23/09/2016.
 */
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.viewmodels.RabbitMessageViewModel;

public class Send {



    public boolean Init(String sWorkspaceId, String sUserId) throws IOException
    {

        //TODO: quando ci saranno i shared workspaces bisognerà aggiungere/caricare le sessioni degli utenti che posso vedere il workspace

        try {
            //load all active session of userId
            SessionRepository oSessionRepo = new SessionRepository();
            List<UserSession> aoUserSessions = oSessionRepo.GetAllActiveSessions(sUserId);
            Connection oConnection = null;
            //Declare queue for send message then send it.
            String sExchangeType = "fanout"; //fanout indica in broadcasting
            String sRoutingKey = "";
            Boolean bExDurable = true;
            try {
                oConnection = RabbitFactory.getConnectionFactory().newConnection();
            } catch (IOException | TimeoutException e) {
                LauncherMain.s_oLogger.debug("Send.Init: " + e.getMessage());
                return false;
            }
            //Create Channel
            Channel oChannel = null;
            try {
                oChannel = oConnection.createChannel();
            } catch (IOException e) {
                LauncherMain.s_oLogger.debug("Send.Init: " + e.getMessage());
                return false;
            }
            try {
                oChannel.exchangeDeclare(sWorkspaceId, sExchangeType, bExDurable);
                //for each session create a binding
                for (UserSession oUserSession : aoUserSessions) {

                    oChannel.queueDeclare(oUserSession.getSessionId(), Boolean.parseBoolean(ConfigReader.getPropValue("RABBIT_QUEUE_DURABLE")), false, false, null);
                    //create binding
                    oChannel.queueBind(oUserSession.getSessionId(), sWorkspaceId, sRoutingKey);
                }

            } catch (IOException e) {
                LauncherMain.s_oLogger.debug("Send.Init: " + e.getMessage());
                return false;
            }
            finally{
                try {
                    oChannel.close();
                } catch (IOException | TimeoutException e) {
                    LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
                    return false;
                }
                try {
                    // ATTENZIONE DEVE FARLO IN TUTTI I CASI
                    oConnection.close();
                } catch (IOException e) {
                    LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
                    return false;
                }
            }
        }
        catch (Exception e) {
            LauncherMain.s_oLogger.debug("Send.Init: " + e.getMessage());
            return false;
        }
        return true;

    }


    public  boolean SendMsg(String sMessageAttribute) throws  IOException {
        return  SendMsgOnQueue(ConfigReader.getPropValue("RABBIT_QUEUE_NAME"),sMessageAttribute);
    }

    public boolean SendMsgOnQueue(String sQueue, String sMessageAttribute) throws IOException
    {
        //create connection to the server
        Connection oConnection = null;
        try {
            oConnection = RabbitFactory.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        //Create Channel
        Channel oChannel=null;
        try {
            oChannel = oConnection.createChannel();
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        //Declare queue for send message then send it.
        try {

            oChannel.queueDeclare(sQueue, Boolean.parseBoolean(ConfigReader.getPropValue("RABBIT_QUEUE_DURABLE")), false, false, null);
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }

        //String message = "Hello World!";
        try {
            oChannel.basicPublish("", sQueue, null, sMessageAttribute.getBytes("UTF-8"));
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        LauncherMain.s_oLogger.debug(" [x] Sent '" + sMessageAttribute + "'");

        try {
            oChannel.close();
        } catch (IOException | TimeoutException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        try {
            // ATTENZIONE DEVE FARLO IN TUTTI I CASI
            oConnection.close();
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }

        return true;

    }


    /**
     *
     * @param sExchangeName (Is the workspace Id)
     * @param sMessageAttribute
     * @return true if the message is sent, false otherwise
     * @throws IOException
     */
    public boolean SendMsgOnExchange(String sExchangeName, String sMessageAttribute) throws IOException
    {
        //create connection to the server
        Connection oConnection = null;
        try {
            oConnection = RabbitFactory.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        //Create Channel
        Channel oChannel= null;
        try {
            oChannel = oConnection.createChannel();
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        //Declare queue for send message then send it.
        String sExchangeType = "fanout"; //fanout indica in broadcasting
        Boolean bExDurable = true;
        String sRoutingKey = "";
        try {

            oChannel.exchangeDeclare(sExchangeName, sExchangeType, bExDurable);

        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }

        //String message = "Hello World!";
        try {
            oChannel.basicPublish(sExchangeName, sRoutingKey, null, sMessageAttribute.getBytes());
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        LauncherMain.s_oLogger.debug(" [x] Sent '" + sMessageAttribute + "'");

        try {
            oChannel.close();
        } catch (IOException | TimeoutException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }
        try {
            // ATTENZIONE DEVE FARLO IN TUTTI I CASI
            oConnection.close();
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendMgs: " + e.getMessage());
            return false;
        }

        return true;

    }

    public boolean SendUpdateProcessMessage(String sWorkspaceId) throws JsonProcessingException {
        LauncherMain.s_oLogger.debug("Send.SendUpdateProcessMessage: Send update message");
        RabbitMessageViewModel oUpdateProcessMessage = new RabbitMessageViewModel();
        oUpdateProcessMessage.setMessageCode(LauncherOperations.UPDATEPROCESSES);
        oUpdateProcessMessage.setWorkspaceId(sWorkspaceId);
        String sJSON = MongoRepository.s_oMapper.writeValueAsString(oUpdateProcessMessage);
        try {
            this.SendMsgOnExchange(sWorkspaceId, sJSON);
        } catch (IOException e) {
            LauncherMain.s_oLogger.debug("Send.SendUpdateProcessMessage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }


}
