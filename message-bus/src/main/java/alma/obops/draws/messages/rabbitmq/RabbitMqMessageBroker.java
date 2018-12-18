package alma.obops.draws.messages.rabbitmq;

import static alma.obops.draws.messages.MessageBroker.now;

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

import alma.obops.draws.messages.AbstractMessageBroker;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.configuration.PersistedEnvelopeRepository;
import alma.obops.draws.messages.configuration.RecipientGroupRepository;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.SimpleEnvelope;
import alma.obops.draws.messages.TimeLimitExceededException;

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

	private PersistenceListener messageLogListener;
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
			this.messageLogListener = new PersistenceListener( this.channel, exchangeName, envelopeRepository );
		} 
		catch (IOException | TimeoutException e) {
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * TODO
	 */
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


	/**
	 * TODO
	 */
	public void drainQueue( MessageQueue queue ) {
		drainQueue( queue.getName() );
	}
	
	
	/**
	 * TODO
	 */
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
		return envelopeRepository;
	}

	public Runnable getMessageLogListener() {
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
		
		if( groupName == null || (!groupName.endsWith( ".*" ))) {
			throw new IllegalArgumentException( "Invalid group name: " + groupName );
		}
		if( queueName == null ) {
			throw new IllegalArgumentException( "Null queueName" );
		}

		Optional<RecipientGroup> oGroup = groupRepository.findByName( groupName );
		RecipientGroup group = oGroup.isPresent() ? oGroup.get() : new RecipientGroup( groupName );
		group.addMember( queueName );
		groupRepository.save( group );
	}

//	/**
//	 * Wait until a message arrives, set its state to {@link State#Received} or
//	 * {@link State#Expired}.
//	 * 
//	 * @param timeLimit
//	 *            If greater than 0 it represents the number of msec to wait for a
//	 *            message to arrive before timing out: upon timeout a
//	 *            {@link TimeLimitExceededException} is thrown.
//	 * 
//	 * @return The message we received.
//	 */
//	// This does not work, no time to understand why -- amchavan, 18-Oct-2018
//	@SuppressWarnings("unused")
//	private Envelope receiveOneEXPERIMENTAL( MessageQueue queue, long timeLimit ) {
//		
//		OneReceiver receiver1 = new OneReceiver( channel, queue, timeLimit );
//		SimpleEnvelope receivedEnvelope = (SimpleEnvelope) receiver1.receive();
//		
//		// See if the message has expired
//		String now = nowISO();
//		final long timeToLive = receivedEnvelope.getTimeToLive();
//		if( timeToLive != 0 ) {
//			receivedEnvelope.setState( State.Received );
//			receivedEnvelope.setReceivedTimestamp( now );
//		}
//		else {
//			receivedEnvelope.setState( State.Expired );
//			receivedEnvelope.setExpiredTimestamp( now );
//		}
//				
//		// Signal the state change to the message log as well
//		try {
//			sendNewStateEvent( receivedEnvelope.getId(), receivedEnvelope.getState(), now );
//		} 
//		catch (IOException e) {
//			throw new RuntimeException( e );
//		}
//		return receivedEnvelope;
//	}
	
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
	
	@Override
	public MessageQueue messageQueue( String queueName ) {
		MessageQueue ret = new MessageQueue( queueName, this );

		try {
			this.channel.queueDeclare( queueName, 
					                   true, 			// durable
					                   false, 			// non-exclusive
					                   false, 			// auto-deleting?
					                   null 			// no extra properties
					                   );
			String routingKey = queueName;
			this.channel.queueBind( queueName, exchangeName, routingKey );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
		return ret;
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
	protected SimpleEnvelope sendOne( MessageQueue queue, Message message, long expireTime ) {
		SimpleEnvelope envelope = super.sendOne( queue, message, expireTime );
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString( envelope );
			
			AMQP.BasicProperties properties = 
					new AMQP.BasicProperties.Builder()
						.deliveryMode( 2 )			// persisted delivery
//						.correlationId( addCorrelationId ? envelope.getId() : null )
//						.replyTo( addCorrelationId ? CALLBACK_MESSAGE_BUS : null )
						.build();
			String routingKey = queue.getName();	// Our API calls "queue" what RabbitMQ calls "routing key"
			this.channel.basicPublish( exchangeName, routingKey, properties, json.getBytes() );
//			this.mainChannel.close();
//			this.mainChannel.getConnection().close();
			return envelope;
		} 
		catch( IOException|TimeLimitExceededException e ) {
			throw new RuntimeException( e );
		}
	}

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
