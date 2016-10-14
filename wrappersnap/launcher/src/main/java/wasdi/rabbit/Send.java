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

public class Send {

    public boolean SendMsg( String sMessageAttribute) throws IOException
    {
        //create connection to the server
        ConnectionFactory oFactory = new ConnectionFactory();
        oFactory.setHost(ConfigReader.getPropValue("RABBIT_HOST"));
        oFactory.setPort(Integer.parseInt(ConfigReader.getPropValue("RABBIT_QUEUE_PORT")));
        Connection oConnection = null;
        try {
            oConnection = oFactory.newConnection();
        } catch (IOException | TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        //Create Channel
        Channel oChannel=null;
        try {
            oChannel = oConnection.createChannel();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        //Declare queue for send message then send it.
        try {

            oChannel.queueDeclare(ConfigReader.getPropValue("RABBIT_QUEUE_NAME"), Boolean.parseBoolean(ConfigReader.getPropValue("RABBIT_QUEUE_DURABLE")), false, false, null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        //String message = "Hello World!";
        try {
            oChannel.basicPublish("", ConfigReader.getPropValue("RABBIT_QUEUE_NAME"), null, sMessageAttribute.getBytes("UTF-8"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        System.out.println(" [x] Sent '" + sMessageAttribute + "'");

        try {
            oChannel.close();
        } catch (IOException | TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        try {
            // ATTENZIONE DEVE FARLO IN TUTTI I CASI
            oConnection.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;

    }

}
