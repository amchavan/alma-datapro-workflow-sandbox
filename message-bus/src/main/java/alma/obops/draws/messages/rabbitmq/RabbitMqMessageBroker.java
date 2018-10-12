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
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.SimpleEnvelope;
import alma.obops.draws.messages.TimeLimitExceededException;

/**
 * @author mchavan 27-Sep-2018
 */
public class RabbitMqMessageBroker extends AbstractMessageBroker implements MessageBroker {

	/** How long to sleep before polling RabbitMQ for the next message */
	private static final long WAIT_BETWEEN_POLLING_FOR_GET = 500L;
	
	static final String LOGGING_MESSAGE_BUS       = "logging-bus";
	static final String MAIN_MESSAGE_BUS          = "main-bus";
	static final String CALLBACK_MESSAGE_BUS      = "callback-bus";
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
	private MessageLogListener messageLogListener;
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
		this.mainChannel.queueDeclare( MAIN_MESSAGE_BUS,     true, false, false, null );
		this.mainChannel.queueDeclare( LOGGING_MESSAGE_BUS,  true, false, false, null );
		this.mainChannel.queueDeclare( CALLBACK_MESSAGE_BUS, true, false, false, null );
		
		this.messageLogListener = new MessageLogListener( this.mainChannel, exchangeName, envelopeRepository );
	}

	/**
	 * FOR TESTING ONLY: remove all log messages from the logging queue
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
	void drainQueue( String rabbitMqMessageQueueName ) {
		
		try {
			this.mainChannel.queuePurge( rabbitMqMessageQueueName );
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
//		String routingKey = "#";
//
//		this.mainChannel.( LOGGING_MESSAGE_BUS, exchangeName, routingKey );
//		GetResponse response = null;
//		while( true ) {
//
//			boolean autoAck = true;
//			response = this.mainChannel.basicGet( LOGGING_MESSAGE_BUS, autoAck );
//			if( response == null ) {
//				break;
//			}
//			
//			byte[] body = response.getBody();
//			String json = new String(body, "UTF-8");
//			System.out.println( ">>> dropping message: " + json );
//		}
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
		RabbitMqMessageQueue rmQueue = (RabbitMqMessageQueue) queue;
		try {
			GetResponse response = null;
			
			// Loop until we receive a message
			while( true ) {

				boolean autoAck = true;
				response = this.mainChannel.basicGet( rmQueue.getRmqQueueName(), autoAck );
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
		return sendOne( queue, message, expireTime, false );
	}
	
	/** 
	 * @param addCorrelationId     Should be <code>true</code> for RPC -- see https://www.rabbitmq.com/tutorials/tutorial-six-java.html
	 */
	public Envelope sendOne( MessageQueue queue, Message message, long expireTime, boolean addCorrelationId ) {

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
						.correlationId( addCorrelationId ? envelope.getId() : null )
						.replyTo( addCorrelationId ? CALLBACK_MESSAGE_BUS : null )
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
		MessageQueue ret = createAndBindMessageQueue( queueName, MAIN_MESSAGE_BUS );
		return ret;
	}
	
	private MessageQueue createAndBindMessageQueue( String queueName, String rmqQueueName ) {
		MessageQueue ret = new RabbitMqMessageQueue( queueName, this, rmqQueueName );

		// Bind the RabbitMQ queue to our "queue" -- the API's 
		// "queue" is implemented by way of RabbitMQ's "routing key"
		String routingKey = queueName;		
		try {
			this.mainChannel.queueBind( rmqQueueName, exchangeName, routingKey );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
		
		return ret;
	}
	
	@Override
	public MessageQueue rpcResponseMessageQueue( String queueName ) {
		MessageQueue ret = createAndBindMessageQueue( queueName, CALLBACK_MESSAGE_BUS );
		return ret;
	}

	@Override
	public Envelope sendRpcRequest( MessageQueue queue, RequestMessage message, long expireTime ) {
		return sendOne( queue, message, expireTime, true );
	}

	void drainLoggingQueue() {
		drainQueue( LOGGING_MESSAGE_BUS );
	}

	@Override
	public Envelope sendRpcResponse( MessageQueue queue, ResponseMessage message, long timeToLive ) {
		return sendOne( queue, message, timeToLive );
	}
}
