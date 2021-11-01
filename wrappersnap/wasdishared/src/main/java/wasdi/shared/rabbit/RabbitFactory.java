package wasdi.shared.rabbit;

import com.rabbitmq.client.ConnectionFactory;

import wasdi.shared.config.WasdiConfig;

import java.io.IOException;

/**
 * Created by s.adamo on 14/02/2017.
 */
public class RabbitFactory {
	private RabbitFactory() {
		// / private constructor to hide the public implicit one 
	}
	
	/**
	 * Rabbit User
	 */
    public static String s_sRABBIT_QUEUE_USER = "user";
    
    /**
     * Rabbit Password
     */
    public static String s_sRABBIT_QUEUE_PWD = "password";
    
    /**
     * Rabbit host
     */
    public static String s_sRABBIT_HOST = "127.0.0.1";
    
    /**
     * Rabbit port
     */
    public static String s_sRABBIT_QUEUE_PORT = "5672";
    
    /**
     * Connection Factory: it is instanciated once
     */
    private static ConnectionFactory m_oConnectionFactory = null;
    
    public static void readConfig() {
        RabbitFactory.s_sRABBIT_QUEUE_USER = WasdiConfig.Current.rabbit.user;
        RabbitFactory.s_sRABBIT_QUEUE_PWD = WasdiConfig.Current.rabbit.password;
        RabbitFactory.s_sRABBIT_HOST = WasdiConfig.Current.rabbit.host;
        RabbitFactory.s_sRABBIT_QUEUE_PORT = WasdiConfig.Current.rabbit.port;    	
    }
    
    /**
     * Get the Rabbit Connnection Factory Object
     * @return
     * @throws IOException
     */
    public static ConnectionFactory getConnectionFactory() throws IOException {
        if (m_oConnectionFactory == null) {
            m_oConnectionFactory = new ConnectionFactory();
            m_oConnectionFactory.setUsername(s_sRABBIT_QUEUE_USER);
            m_oConnectionFactory.setPassword(s_sRABBIT_QUEUE_PWD);
            m_oConnectionFactory.setHost(s_sRABBIT_HOST);
            m_oConnectionFactory.setPort(Integer.parseInt(s_sRABBIT_QUEUE_PORT));
        }

        return m_oConnectionFactory;
    }
}
