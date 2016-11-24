package wasdi.rabbit;

/**
 * Created by s.adamo on 23/09/2016.
 */
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import wasdi.ConfigReader;
import wasdi.LauncherMain;

public class Send {

    public  boolean SendMsg(String sMessageAttribute) throws  IOException {
        return  SendMsg(ConfigReader.getPropValue("RABBIT_QUEUE_NAME"),sMessageAttribute);
    }

    public boolean SendMsg(String sQueue, String sMessageAttribute) throws IOException
    {
        //create connection to the server
        ConnectionFactory oFactory = new ConnectionFactory();
        oFactory.setUsername(ConfigReader.getPropValue("RABBIT_QUEUE_USER"));
        oFactory.setPassword(ConfigReader.getPropValue("RABBIT_QUEUE_PWD"));
        oFactory.setHost(ConfigReader.getPropValue("RABBIT_HOST"));
        oFactory.setPort(Integer.parseInt(ConfigReader.getPropValue("RABBIT_QUEUE_PORT")));
        //oFactory.setVirtualHost("/ws");
        Connection oConnection = null;
        try {
            oConnection = oFactory.newConnection();
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

}
