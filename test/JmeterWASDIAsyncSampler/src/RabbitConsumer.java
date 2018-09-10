import com.rabbitmq.client.*;
//JMeter imports 
import com.rabbitmq.client.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RabbitConsumer{

	private Queue queue;
	private Connection conn; 
	private Channel channel;
	private Consumer consumer;
	private String queueName;
	private String sAPI;
	private String sWsId;
	private String sRhost;
	private String sRPort;
	private String sRuser;
	private String sRpwd;

	private static final Logger LOGGER = 
			LoggerFactory.getLogger(RabbitConsumer.class);


	public RabbitConsumer (String sRhost,String sRPort, String sRuser, String sRpwd,String sAPI,String sWsId) throws IOException, TimeoutException{
		  
		this.sAPI=sAPI;
		this.sWsId=sWsId;
		this.sRhost=sRhost;
		this.sRPort=sRPort;
		this.sRuser=sRuser;
		this.sRpwd=sRpwd;
		
		this.queue=new Queue();
		init();
	}



	public void init() throws IOException,TimeoutException  {
		//
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		//wsID is the rabbitMQ routingkey
		String sRoutingKey=this.sWsId;


		//try {
			ConnectionFactory cf = null;
			cf = new ConnectionFactory();
			//get from properties file
			cf.setUsername(this.sRuser);
			cf.setPassword(this.sRpwd);
			cf.setHost(sRhost);
			cf.setPort(Integer.parseInt(sRPort));
			if (cf != null) {
				this.conn = cf.newConnection();
				System.out.println("Created connection trying to open a channel");
				this.channel = conn.createChannel();

				this.channel.exchangeDeclare("amq.topic", "topic", true);
				this.queueName = this.channel.queueDeclare().getQueue();
				// queue,exchange,routingKey
				this.channel.queueBind(queueName, "amq.topic", sRoutingKey);

				this.consumer = new DefaultConsumer(channel) {

					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,	
							byte[] body) throws IOException {

						JSONParser jsonParser = new JSONParser();
						String message = new String(body, "UTF-8");
						RabbitMessage rMs = new RabbitMessage();

						try {
							JSONObject jsonRabbit = (JSONObject) jsonParser.parse(message);
							String messageCode = (String) jsonRabbit.get("messageCode");
							String messageResult="";
							if(jsonRabbit.get("messageResult")!=null) {
								messageResult = (String)jsonRabbit.get("messageResult");
							}
							String messagePayload ="";
							if (String.class.isInstance(jsonRabbit.get("payload")) ){ 
								messagePayload = (String) jsonRabbit.get("payload");
							}
							System.out.println(messagePayload);
							RabbitConsumer.LOGGER.info(messagePayload);

							
							rMs.setsMessageCode(messageCode);
							rMs.setsMessageResult(messageResult);
							//RabbitConsumer.LOGGER.debug(messageCode+" "+messagePayload);
							System.out.println(messageResult);
							queue.add(rMs);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				};
			}
		//}

		//catch (TimeoutException e){e.printStackTrace();}

	}


	public boolean runTest() {
		boolean response= false;
		boolean exit= false;
		try {				
			channel.basicConsume(queueName, true, this.consumer);
		}catch (IOException e) {
			RabbitConsumer.LOGGER.error("IO error reading from queue");
		}
		RabbitMessage rem;

		while (exit!=true) {
			try {
				synchronized (this.queue) {
					while (this.queue.getSize() == 0) {
						RabbitConsumer.LOGGER.info("The Queue is empty");
						this.queue.wait();
					}
					rem = this.queue.pop();
				}

				if (rem.getsMessageCode().equals(this.sAPI)) {
					exit=true;
					switch (rem.getsMessageResult()) {
					case "OK":
						RabbitConsumer.LOGGER.info("MessageResult: OK");
						System.out.println(rem.getsMessageCode());
						System.out.println(rem.getsMessageResult());
						response= true;
						break;

					case "KO":
						RabbitConsumer.LOGGER.info("MessageResult: KO");
						System.out.println(rem.getsMessageCode());
						System.out.println(rem.getsMessageResult());
						response= false;
						break;
					default:
							RabbitConsumer.LOGGER.info("MessageResult: KO");
							break; //finished switch branch true
					}
				}
	
			}
			catch (InterruptedException e) {
				break; //exit while true
			}
		}
		try {
			this.channel.close();
			conn.close();
			return response;
		}
		catch (IOException | TimeoutException e) {
			 e.printStackTrace();
		}
		
		return response;
	}
	public static void main (String[] args) {
		try {
			RabbitConsumer rc =new RabbitConsumer("92.223.159.5","19999","fadeout","fadeout","DOWNLOAD","1b845fc6-bf72-44e2-99b6-26b33f529365");
			rc.runTest();
		}
		catch(TimeoutException | IOException e) {
			RabbitConsumer.LOGGER.error(e.getMessage());
		}
	
	}

}
