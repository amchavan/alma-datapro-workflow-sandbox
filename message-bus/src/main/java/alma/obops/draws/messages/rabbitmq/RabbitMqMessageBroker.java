package alma.obops.draws.messages.rabbitmq;

import static alma.obops.draws.messages.MessageBroker.now;
import static alma.obops.draws.messages.MessageBroker.nowISO;
import static alma.obops.draws.messages.MessageBroker.ourIP;

import java.io.IOException;
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
import com.rabbitmq.client.GetResponse;

import alma.obops.draws.messages.AbstractMessageBroker;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
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
	
	static final String MESSAGE_PERSISTENCE_QUEUE = "message.persistence.queue";
	static final String MESSAGE_STATE_ROUTING_KEY = "new.message.state";
	
	public static Channel makeChannelAndExchange( String url, String exchangeName ) throws IOException, java.util.concurrent.TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( url );
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		channel.exchangeDeclare( exchangeName, BuiltinExchangeType.TOPIC );
		return channel;
	}
	
	private String baseURL;
	private String ourIP;
//	private Channel messageLogChannel;
	private PersistenceListener messageLogListener;
	private String exchangeName;
	private Channel mainChannel;
	private RecipientGroupRepository groupRepository;
	
	public RabbitMqMessageBroker( String baseURL, 
								  String exchangeName, 
								  PersistedEnvelopeRepository envelopeRepository,
								  RecipientGroupRepository groupRepository ) throws IOException, TimeoutException {
		this.baseURL = baseURL;
		this.exchangeName = exchangeName;
		this.ourIP = ourIP();
		this.groupRepository = groupRepository;
		
		// Declare the main channel and queues
		this.mainChannel = makeChannelAndExchange( baseURL, exchangeName );
		this.mainChannel.queueDeclare( MESSAGE_PERSISTENCE_QUEUE,  true, false, false, null );
		
		this.messageLogListener = new PersistenceListener( this.mainChannel, exchangeName, envelopeRepository );
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
			this.mainChannel.queuePurge( queueName );
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}


	/**
	 * TODO
	 */
	public void deleteQueue( MessageQueue queue ) {
		try {
			this.mainChannel.queueDelete( queue.getName() );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
	
	
	public String getBaseURL() {
		return this.baseURL;
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

	@Override
	// TODO: pull this up to AbstractMessageBroker, need to harmonize
	//       with CouchDbMessageBroker -- only difference is how 
	//		 envelope state changes are persisted
	public void listen( MessageQueue queue, MessageConsumer consumer, int timeout, boolean justOne ) throws IOException {
		if( queue == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		while( true ) {
			
			// Receive the next message, consume it, and mark it as such
			SimpleEnvelope envelope = (SimpleEnvelope) receive( queue, timeout );

//			System.out.println( ">>>>> queueName: " + queueName );
//			System.out.println( ">>>>> envelope: " + envelope );
//			System.out.println( ">>>>> message: " + envelope.getMessage() );
			consumer.consume( envelope.getMessage() );

			String now = nowISO();
			envelope.setState( State.Consumed );
			envelope.setConsumedTimestamp( now );
						
			// Signal the state change to the message log as well
			sendNewStateEvent( envelope.getId(), envelope.getState(), now );
			
			if( justOne ) {
				break;
			}
		}
	}

	@Override
	public Envelope receive( MessageQueue queue, long timeLimit )
			throws IOException, TimeLimitExceededException {
		
		// Wait for the first non-expired message we get, and return it
		while( true ) {
			Envelope receivedEnvelope = receiveOne( queue, timeLimit );
			if( receivedEnvelope.getState() != State.Expired ) {
				return (Envelope) receivedEnvelope;
			}
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
	private Envelope receiveOne( MessageQueue queue, long timeLimit ) {
		SimpleEnvelope receivedEnvelope;
		Date callTime = now();
		try {
			GetResponse response = null;
			
			// Loop until we receive a message
			while( true ) {

				boolean autoAck = true;
				response = this.mainChannel.basicGet( queue.getName(), autoAck );
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

			// See if the message has expired
			String now = nowISO();
			final long timeToLive = receivedEnvelope.getTimeToLive();
			if( timeToLive != 0 ) {
				receivedEnvelope.setState( State.Received );
				receivedEnvelope.setReceivedTimestamp( now );
			}
			else {
				receivedEnvelope.setState( State.Expired );
				receivedEnvelope.setExpiredTimestamp( now );
			}
					
			// Signal the state change to the message log as well
			sendNewStateEvent( receivedEnvelope.getId(), receivedEnvelope.getState(), now );
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
		return receivedEnvelope;
	}
	
	private void sendNewStateEvent( String id, State state, String timestamp ) throws IOException {
		String stateChange = id + "@" + state.toString() + "@" + timestamp;
		this.mainChannel.basicPublish( this.exchangeName, 
				                  	   MESSAGE_STATE_ROUTING_KEY, 
				                 	   null, 
				                 	   stateChange.getBytes() );
	}
	
	@Override
	public Envelope sendOne( MessageQueue queue, Message message, long expireTime ) {

		try {
			SimpleEnvelope envelope = new SimpleEnvelope( message, this.ourIP, queue.getName(), expireTime );
			envelope.setSentTimestamp( nowISO() );
			envelope.setState( State.Sent );
			envelope.setMessageClass( message.getClass().getName() );
			envelope.setExpireTime( expireTime );
			message.setEnvelope( envelope );
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString( envelope );
			
			AMQP.BasicProperties properties = 
					new AMQP.BasicProperties.Builder()
						.deliveryMode( 2 )			// persisted delivery
//						.correlationId( addCorrelationId ? envelope.getId() : null )
//						.replyTo( addCorrelationId ? CALLBACK_MESSAGE_BUS : null )
						.build();
			String routingKey = queue.getName();	// The API calls "queue" what RabbitMQ calls "routing key"
			this.mainChannel.basicPublish( exchangeName, routingKey, properties, json.getBytes() );
//			this.mainChannel.close();
//			this.mainChannel.getConnection().close();
			return envelope;
		} 
		catch( IOException|TimeLimitExceededException e ) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public MessageQueue messageQueue( String queueName ) {
		MessageQueue ret = new MessageQueue( queueName, this );

		try {
			this.mainChannel.queueDeclare( queueName, 
					                       true, 			// durable
					                       false, 			// non-exclusive
					                       false, 			// not auto-deleting
					                       null 			// no extra properties
					                       );
			String routingKey = queueName;
			this.mainChannel.queueBind( queueName, exchangeName, routingKey );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
		return ret;
	}
	
	void drainLoggingQueue() {
		drainQueue( MESSAGE_PERSISTENCE_QUEUE );
	}
}