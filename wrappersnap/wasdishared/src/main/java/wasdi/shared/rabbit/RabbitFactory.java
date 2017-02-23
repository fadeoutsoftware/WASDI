package wasdi.shared.rabbit;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

/**
 * Created by s.adamo on 14/02/2017.
 */
public class RabbitFactory {

    public static String s_sRABBIT_QUEUE_USER = "fadeout";
    public static String s_sRABBIT_QUEUE_PWD = "fadeout";
    public static String s_sRABBIT_HOST = "127.0.0.1";
    public static String s_sRABBIT_QUEUE_PORT = "5672";

    private static ConnectionFactory m_oConnectionFactory = null;

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
