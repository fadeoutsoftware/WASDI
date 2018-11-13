package wasdi.rabbit;

import com.rabbitmq.client.ConnectionFactory;
import wasdi.ConfigReader;
import java.io.IOException;

/**
 * Created by s.adamo on 02/02/2017.
 */
public class RabbitFactory {

    private static ConnectionFactory m_oConnectionFactory = null;

    public static ConnectionFactory getConnectionFactory() throws IOException {
        if (m_oConnectionFactory == null) {
            m_oConnectionFactory = new ConnectionFactory();
            m_oConnectionFactory.setUsername(ConfigReader.getPropValue("RABBIT_QUEUE_USER"));
            m_oConnectionFactory.setPassword(ConfigReader.getPropValue("RABBIT_QUEUE_PWD"));
            m_oConnectionFactory.setHost(ConfigReader.getPropValue("RABBIT_HOST"));
            m_oConnectionFactory.setPort(Integer.parseInt(ConfigReader.getPropValue("RABBIT_QUEUE_PORT")));
        }

        return m_oConnectionFactory;
    }


}
