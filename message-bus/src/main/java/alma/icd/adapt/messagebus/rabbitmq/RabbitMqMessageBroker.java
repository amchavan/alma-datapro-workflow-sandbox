package alma.icd.adapt.messagebus.rabbitmq;

import static alma.icd.adapt.messagebus.MessageBroker.now;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.GetResponse;

import alma.icd.adapt.messagebus.AbstractMessageBroker;
import alma.icd.adapt.messagebus.Envelope;
import alma.icd.adapt.messagebus.Message;
import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.MessageConsumer;
import alma.icd.adapt.messagebus.MessageQueue;
import alma.icd.adapt.messagebus.SimpleEnvelope;
import alma.icd.adapt.messagebus.TimeLimitExceededException;
import alma.icd.adapt.messagebus.Envelope.State;
import alma.icd.adapt.messagebus.configuration.PersistedEnvelopeRepository;
import alma.icd.adapt.messagebus.configuration.RecipientGroupRepository;

/**
 * @author mchavan 27-Sep-2018
 */
public class RabbitMqMessageBroker extends AbstractMessageBroker implements MessageBroker {
	
	/** How long to sleep before polling RabbitMQ for the next message */
	private static final long WAIT_BETWEEN_POLLING_FOR_GET = 500L;
	
	/** Minimum valid RabbitMQ URI */
	public static final String MINIMAL_URI = "amqp://@";
	
	public static final String MESSAGE_PERSISTENCE_QUEUE = "message.persistence.queue";
	public static final String MESSAGE_STATE_ROUTING_KEY = "new.message.state";
	
	private static String makeQueueName( String serviceName, String queueName ) {
		return serviceName + "." + queueName;
	}
	
	private MessageArchiver messageLogListener;
	private String exchangeName;
	private Channel channel;
	private RecipientGroupRepository groupRepository;
	private PersistedEnvelopeRepository envelopeRepository;

	private Date lastDeliveryTime;
	
	/**
	 * Constructor for default connection on localhost
	 * 
	 * @param exchangeName       Name of the RabbitMQ exchange to use, will be
	 *                           created if necessary
	 *                           
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public RabbitMqMessageBroker( String exchangeName,
			  					  PersistedEnvelopeRepository envelopeRepository,
			  					  RecipientGroupRepository groupRepository ) throws IOException, TimeoutException {
		this( MINIMAL_URI, null, null, exchangeName, envelopeRepository, groupRepository );
	}
	
	/**
	 * Constructor, will use exchange {@value #DEFAULT_EXCHANGE_NAME}, creating it
	 * if necessary.
	 * 
	 * @param baseURI      Base RabbitMQ URI, for instance
	 *                     {@code "amqp://eso.org:1234"}
	 * @param username     Username for the RabbitMQ server
	 * @param password     Password for the RabbitMQ server
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public RabbitMqMessageBroker( String baseURI,
			  					  String username,
			  					  String password,
			  					  PersistedEnvelopeRepository envelopeRepository,
			  					  RecipientGroupRepository groupRepository ) {
		this( baseURI, username, password, 
			  MessageBroker.DEFAULT_MESSAGE_BROKER_NAME, 
			  envelopeRepository, groupRepository );
	}

	/**
	 * Constructor
	 * 
	 * @param baseURI            Base RabbitMQ URI, for instance
	 *                           {@code "amqp://eso.org:1234"}
	 * @param username           Username for the RabbitMQ server
	 * @param password           Password for the RabbitMQ server
	 * @param exchangeName       Name of the RabbitMQ exchange to use, will be
	 *                           created if necessary
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public RabbitMqMessageBroker( String baseURI,
								  String username,
								  String password,
								  String exchangeName,
								  PersistedEnvelopeRepository envelopeRepository,
								  RecipientGroupRepository groupRepository ) {

		if( baseURI == null ) {
			throw new RuntimeException( "Arg baseURI cannot be null" );
		}

		ConnectionFactory factory = new ConnectionFactory();
		try {
			factory.setUri( baseURI );
		} 
		catch( URISyntaxException | NoSuchAlgorithmException | KeyManagementException e ) {
			throw new RuntimeException( e );
		}
		
		if( username != null && password != null ) {
			factory.setUsername( username );
			factory.setPassword( password );
		}
				
		this.exchangeName = exchangeName;
		this.groupRepository = groupRepository;
		this.envelopeRepository = envelopeRepository;
		
		try {
			Connection connection = factory.newConnection();
			this.channel = connection.createChannel();
			this.channel.exchangeDeclare( exchangeName, BuiltinExchangeType.TOPIC );
			this.channel.queueDeclare( MESSAGE_PERSISTENCE_QUEUE,  true, false, false, null );
			this.messageLogListener = new MessageArchiver( this.channel, exchangeName, envelopeRepository );
		} 
		catch (IOException | TimeoutException e) {
			throw new RuntimeException( e );
		}
	}


	@Override
	public void closeConnection() {
		try {
			getChannel().getConnection().close();
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void deleteQueue( MessageQueue queue ) {
		try {
			this.channel.queueDelete( queue.getName() );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
	
	// FOR TESTING ONLY
	public void drainLoggingQueue() {
		drainQueue( MESSAGE_PERSISTENCE_QUEUE );
	}

	/** For testing only */
	public void drainQueue( MessageQueue queue ) {
		drainQueue( queue.getName() );
	}

	/** For testing only */
	void drainQueue( String queueName ) {
		try {
			this.channel.queuePurge( queueName );
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	// For testing only
	Channel getChannel() {
		return channel;
	}

	public PersistedEnvelopeRepository getEnvelopeRepository() {
		return this.envelopeRepository;
	}
	
	public Runnable getMessageArchiver() {
		return this.messageLogListener;
	}
	
	@Override
	public List<String> groupMembers(String groupName) throws IOException {
		if( groupName == null || (!groupName.endsWith( ".*" ))) {
			throw new IllegalArgumentException( "Invalid group name: " + groupName );
		}

		Optional<RecipientGroup> oGroup = groupRepository.findByName( groupName );
		if( oGroup.isPresent() ) {
			return oGroup.get().getGroupMembersAsList();
		}
		return null;
	}
	
	@Override
	public void joinGroup( String queueName, String groupName ) {
		// no-op
	}

	@Override
	public void listen( MessageQueue queue, MessageConsumer consumer, int timeout ) throws IOException {

		if( queue == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		boolean autoAck = false;
		lastDeliveryTime = new Date();
		RabbitMqMessageBroker broker = this;
		
		DefaultConsumer rmqConsumer = new DefaultConsumer( channel ) {

			@Override
			public void handleDelivery( String consumerTag, 	
										com.rabbitmq.client.Envelope envelope,
										AMQP.BasicProperties properties, 
										byte[] body ) throws IOException {

				lastDeliveryTime = new Date();
				
				String json = new String( body, "UTF-8" );
				ObjectMapper objectMapper = new ObjectMapper();
				SimpleEnvelope receivedEnvelope = objectMapper.readValue( json, SimpleEnvelope.class );
				
				if( !autoAck ) {
                    channel.basicAck(envelope.getDeliveryTag(), false);
				}
				
				computeState( queue, receivedEnvelope );
				if( receivedEnvelope.getState() == State.Received ) {	
					consumer.consume( receivedEnvelope.getMessage() );
					broker.setState( receivedEnvelope, State.Consumed );
				}
			}
		};
		
		// Start waiting for delivered messages, then consume them
		@SuppressWarnings("unused")
		String consumerTag = this.channel.basicConsume( queue.getName(), autoAck, rmqConsumer );
		
		// Timeout loop: check if too much time has passed
		while( true ) {
			Date now = new Date();
			if( timeout > 0 && (now.getTime() - lastDeliveryTime.getTime()) >= timeout ) {
				// Timeout! 
//				System.out.println( "Timeout!" );
				throw new TimeLimitExceededException( "After " + timeout + "msec" );
			}
			MessageBroker.sleep( 100L );
		}
	}
	
	/**
	 * @param queueName   Will be used as the AMPQ routing key; we'll generate the
	 *                    actual RabbitMQ queue name from this parameter and the
	 *                    service name.
	 * 
	 * @param serviceName Identifies the service (application) that's subscribing,
	 *                    as multiple services could subscribe to the same messages.
	 *                    <br>
	 *                    Must be a valid C/Python/Java variable name. <br>
	 *                    Must be unique system-wide.
	 */
	@Override
	public MessageQueue messageQueue( String queueName, String serviceName ) {
		
		try {
			String routingKey = queueName;
			queueName = makeQueueName( serviceName, queueName );
			
			this.channel.queueDeclare( 
					queueName, 
					true, 			// persisted
					false, 			// non-exclusive
					false, 			// auto-deleting?
					null 			// no extra properties
					);
		
			this.channel.queueBind( queueName, exchangeName, routingKey );
		
			MessageQueue ret = new MessageQueue( queueName, serviceName, this );
			return ret;
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Wait until a message arrives, set its state to {@link State#Received} or
	 * {@link State#Expired}.
	 * 
	 * @param timeLimit
	 *            If greater than 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out: upon timeout a
	 *            {@link TimeLimitExceededException} is thrown.
	 * 
	 * @return The message we received.
	 */
	@Override
	protected SimpleEnvelope receiveOne( MessageQueue queue, long timeLimit ) {
		SimpleEnvelope receivedEnvelope;
		Date callTime = now();
		try {
			GetResponse response = null;
			
			// Loop until we receive a message
			while( true ) {
	
				boolean autoAck = true;
				response = this.channel.basicGet( queue.getName(), autoAck );
				if( response != null ) {
					break;
				}
				// Did we time out?
				Date now = now();
				if( timeLimit > 0 && (now.getTime() - callTime.getTime()) > timeLimit ) {
					// YES, throw an exception and exit
					throw new TimeLimitExceededException( "After " + timeLimit + "msec" );
				}
				MessageBroker.sleep( WAIT_BETWEEN_POLLING_FOR_GET );
			}
			
			byte[] body = response.getBody();
			String json = new String(body, "UTF-8");
		
			ObjectMapper objectMapper = new ObjectMapper();
			receivedEnvelope = objectMapper.readValue( json, SimpleEnvelope.class );
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
		return receivedEnvelope;
	}

	@Override
	public Envelope send( String queueName, Message message, long expireTime ) {

		if( queueName == null || queueName.length() == 0 || message == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		Envelope ret = this.sendOne( queueName, message, expireTime );
		return ret;
	}

	@Override
	protected SimpleEnvelope sendOne( String queueName, Message message, long expireTime ) {
		SimpleEnvelope envelope = super.sendOne( queueName, message, expireTime );
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString( envelope );
			
			AMQP.BasicProperties properties = 
					new AMQP.BasicProperties.Builder()
						.deliveryMode( 2 )			// persisted delivery
						.build();
			String routingKey = queueName;
			this.channel.basicPublish( exchangeName, routingKey, properties, json.getBytes() );
//			this.mainChannel.close();
//			this.mainChannel.getConnection().close();
			return envelope;
		} 
		catch( IOException|TimeLimitExceededException e ) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected SimpleEnvelope sendOne( MessageQueue queue, Message message, long expireTime ) {
		SimpleEnvelope envelope = super.sendOne( queue, message, expireTime );
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString( envelope );
			
			AMQP.BasicProperties properties = 
					new AMQP.BasicProperties.Builder()
						.deliveryMode( 2 )			// persisted delivery
						.build();
			String routingKey = queue.getName();
			this.channel.basicPublish( exchangeName, routingKey, properties, json.getBytes() );
//			this.mainChannel.close();
//			this.mainChannel.getConnection().close();
			return envelope;
		} 
		catch( IOException|TimeLimitExceededException e ) {
			throw new RuntimeException( e );
		}
	}

//	@Override
//	public void setServiceName( String serviceName ) {
//		if( serviceName == null || serviceName.length() == 0 ) {
//			serviceName = "noservice";
//		}
//		
//		if( ! serviceName.matches( "^[a-zA-Z_][a-zA-Z_0-9]*$" ) ) {
//			throw new RuntimeException( "Invalid serviceName" );
//		}
//		this.serviceName = serviceName;
//	}

	/**
	 * Subclassed to support sending a "state change" event, to persist the envelope
	 * state
	 */
	@Override
	protected String setState(Envelope envelope, State state) throws IOException {
		String timestamp = super.setState(envelope, state);
		String stateChange = envelope.getId() + "@" + state.toString() + "@" + timestamp;
		this.channel.basicPublish( this.exchangeName, 
				                  	   MESSAGE_STATE_ROUTING_KEY, 
				                 	   null, 
				                 	   stateChange.getBytes() );
		return timestamp;
	}
}
