package wasdi.shared.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
//import org.apache.log4j.Logger;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.WorkspaceRepository;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by s.adamo on 14/02/2017.
 */
public class RabbitMethods {

    //static Logger s_oLogger = Logger.getLogger(RabbitMethods.class);

    /**
     *
     * @param sExchange: matches with workspace
     * @return boolean value: true ok, false otherwise
     */
    public static boolean ExchangeDelete(String sExchange)
    {
        //create connection to the server
        Connection oConnection = null;
        try {
            oConnection = RabbitFactory.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException oEx) {
            oEx.printStackTrace();
            //s_oLogger.debug("RabbitMethods.ExchangeDelete: Error creating connection" + oEx.getMessage());
            return false;
        }
        //Create Channel
        Channel oChannel;
        try {
            oChannel = oConnection.createChannel();
        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.ExchangeDelete: Error creating channel" + oEx.getMessage());
            oEx.printStackTrace();
            return false;
        }
        try {
            boolean bIfUnused = false;
            AMQP.Exchange.DeleteOk oResult = oChannel.exchangeDelete(sExchange, bIfUnused);
            if (oResult == null)
                return false;

            return true;

        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.ExchangeDelete: Error deleting exchange" + oEx.getMessage());
            oEx.printStackTrace();
        }

        try {
            oChannel.close();
        } catch (IOException | TimeoutException oEx) {
            //s_oLogger.debug("RabbitMethods.ExchangeDelete: Error closing channel" + oEx.getMessage());
            oEx.printStackTrace();
        }
        try {
            // ATTENZIONE DEVE FARLO IN TUTTI I CASI
            oConnection.close();
        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.ExchangeDelete: Error closing connection" + oEx.getMessage());
            oEx.printStackTrace();
        }

        return false;

    }

    /**
     *
     * @param sQueue: matches with user session Id
     * @return boolean value: true ok, false otherwise
     */
    public static boolean QueueDelete(String sQueue, String sUserId)
    {
        //create connection to the server
        Connection oConnection = null;
        try {
            oConnection = RabbitFactory.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException oEx) {
            //s_oLogger.debug("RabbitMethods.QueueDelete: Error creating connection" + oEx.getMessage());
            oEx.printStackTrace();
            return false;
        }
        //Create Channel
        Channel oChannel;
        try {
            oChannel = oConnection.createChannel();
        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.QueueDelete: Error creating channel" + oEx.getMessage());
            oEx.printStackTrace();
            return false;
        }
        try {
            boolean bIfUnused = false;
            boolean bIfEmpty = false;
            String sRoutingKey = "";
            //queue delete
            AMQP.Queue.DeleteOk oResult = oChannel.queueDelete(sQueue, bIfUnused, bIfEmpty);
            if (oResult == null)
                return false;

            //unbinding queue from all exchange (workspaces) of the user
            WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
            List<Workspace> aoWorkspaces = oWorkspaceRepository.GetWorkspaceByUser(sUserId);
            for (Workspace oWorkspace :
                    aoWorkspaces) {
                try {
                    //purge message on queue
                    oChannel.queuePurge(sQueue);
                    //unbind queue
                    oChannel.queueUnbind(sQueue, oWorkspace.getWorkspaceId(), sRoutingKey);
                }catch (Exception oEx){
                    //s_oLogger.debug("RabbitMethods.QueueDelete: Error unbinding queue" + oEx.getMessage());
                    oEx.printStackTrace();

                }
            }

            return true;

        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.QueueDelete: Error deleting queue" + oEx.getMessage());
            oEx.printStackTrace();

        }

        try {
            oChannel.close();
        } catch (IOException | TimeoutException oEx) {
            //s_oLogger.debug("RabbitMethods.QueueDelete: Error closing channel" + oEx.getMessage());
            oEx.printStackTrace();

        }
        try {
            // ATTENZIONE DEVE FARLO IN TUTTI I CASI
            oConnection.close();
        } catch (IOException oEx) {
            //s_oLogger.debug("RabbitMethods.QueueDelete: Error closing connection" + oEx.getMessage());
            oEx.printStackTrace();

        }

        return false;

    }
}
