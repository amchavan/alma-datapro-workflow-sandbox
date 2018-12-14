package alma.obops.draws.messages;

import static alma.obops.draws.messages.MessageBroker.nowISO;
import static alma.obops.draws.messages.MessageBroker.ourIP;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.security.TokenFactory;

/**
 * @author mchavan 10-Oct-2018
 */
public abstract class AbstractMessageBroker implements MessageBroker {

	protected TokenFactory tokenFactory = null;
	protected String sendToken;			// Token to send with our messages
	protected String ourIP;
	protected List<String> acceptedRoles;

	public AbstractMessageBroker() {
		this.tokenFactory = null;
		this.sendToken = null;
		this.ourIP = ourIP();
	}

	/** 
	 * Compute what the state of the input envelope should be
	 */
	protected void computeState( SimpleEnvelope envelope ) throws IOException {
		
		// -----------------------------------------------------
		// NOTE: use "this" to call subclassed setState() method 
		// -----------------------------------------------------
		
		// Has the message expired?
		long timeToLive = envelope.getTimeToLive();
		if( timeToLive == 0 ) {
			this.setState( envelope, State.Expired );
			return;
		}
		
		// Should we reject the message?
		if( isRejected( envelope )) {
			this.setState( envelope, State.Rejected );
			return;
		}
		
		// Got a valid envelope, mark it as Received 
		this.setState( envelope, State.Received );
	}

	@Override
	public void deleteQueue(MessageQueue queue) {
		// no-op
	}

	/** 
	 * Initialize the envelope with some standard info
	 * @param message
	 * @param envelope
	 */
	protected void initEnvelope( SimpleEnvelope envelope ) {
		envelope.setSentTimestamp( nowISO() );
		envelope.setState( State.Sent );
		envelope.setMessageClass( envelope.getMessage().getClass().getName() );
		envelope.setToken( sendToken );
	}

	/**
	 * Decide whether a message should be rejected:
	 * <ul>
	 * <li> if this broker is not secured the message should not be rejected
	 * <li> if this broker is secured:
	 * <ul> 
	 * 	<li> is the token valid? 
	 * 	<li> do sender roles match any accepted roles? 
	 * 	<li> <strong>TODO</strong> do we accept this message type? 
	 * </ul> 
	 * </ul> 
	 * 
	 * @return <code>true</code> if this message should be rejected,
	 *         <code>false</code> otherwise.
	 */
	protected boolean isRejected( SimpleEnvelope envelope ) {
		
		// Is this broker secured? 
		if( tokenFactory == null ) {
			return false;				// not secured, no rejection
		}
		
		// Is the token valid?
		String token = envelope.getToken();
		Map<String, Object> claims;
		try {
			claims = tokenFactory.decode( token );
		}
		catch( Exception e ) {
			return true;
		}
		
		// Do we have role restrictions?
		if( this.acceptedRoles == null ) {
			return false;		// NO, no rejection
		}
		
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) claims.get( "roles" );
		for( String role: roles ) {
			for( String acceptedRole: this.acceptedRoles ) {
				if( role.equals( acceptedRole )) {
					return false;
				}
			}
		}
		
		// No role found, reject
		return true;
	}
	
	@Override
	public Thread listenInThread( MessageQueue queue, MessageConsumer consumer, int timeout ) {
		
		Runnable receiver = () -> {	
			try {
				this.listen( queue, consumer, timeout );
			}
			catch ( TimeLimitExceededException e ) {
				// ignore
			}
			catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		};
		Thread t = new Thread( receiver );
		t.start();
		return t;
	}

	@Override
	public MessageQueue messageQueue( String queueName ) {
		MessageQueue ret = new MessageQueue( queueName, this );
		return ret;
	}
	
	@Override
	public Envelope receive( MessageQueue queue ) throws IOException {
		return receive( queue, 0 );
	}
	
	
	@Override
	public Envelope receive( MessageQueue queue, long timeLimit )
			throws IOException, TimeLimitExceededException {
		
		// Wait for the first valid message we get and return it
		while( true ) {
			SimpleEnvelope envelope = receiveOne( queue, timeLimit );
			computeState( envelope );
			if( envelope.getState() == State.Received ) {
				return envelope;
			}
		}
	}
	
	
	/**
	 * Wait until a message arrives and return its {@link Envelope}. 
	 * <p>
	 * Implementation is broker-specific and delegated to subclasses.
	 * 
	 * @param timeLimit If greater than 0 it represents the number of msec to wait
	 *                  for a message to arrive before timing out: upon timeout a
	 *                  {@link TimeLimitExceededException} is thrown.
	 */
	protected abstract SimpleEnvelope receiveOne( MessageQueue queue, long timeLimit );

	@Override
	public Envelope send( MessageQueue queue, Message message ) {
		return send( queue, message, 0 );
	}

	@Override
	public Envelope send( MessageQueue queue, Message message, long expireTime ) {

		if( queue == null || message == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		// Are we sending to a group?
		if( ! queue.getName().endsWith( ".*" )) {
			Envelope ret = this.sendOne( queue, message, expireTime );	// No, just send this message
			return ret;
		}
		
		// We are sending to a group: loop over all recipients
		String groupName = queue.getName();
		try {
			List<String> members = groupMembers( groupName );
			if( members == null ) {
				throw new RuntimeException("Receiver group '" + groupName + "' not found");
			}
			Envelope ret = null;
			for( String member: members ) {
				MessageQueue memberQueue = new MessageQueue( member, this );
				ret = this.sendOne( memberQueue, message, expireTime );
			}
			return ret;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	
	
	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it to a queue.<br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * <p>
	 * Implementation is broker-specific and delegated to subclasses, overriding methods
	 * should invoke this method.
	 * 
	 * @param queue      Name of the queue should not end with <code>.*</code> (that
	 *                   is, it should not be a receiver group designator)
	 * 
	 * @param expireTime Time interval before this instance expires, in msec; if
	 *                   timeToLive=0 this instance never expires
	 */
	protected SimpleEnvelope sendOne( MessageQueue queue, Message message, long expireTime ) {
		SimpleEnvelope envelope = new SimpleEnvelope( message, this.ourIP, queue.getName(), expireTime );
		initEnvelope( envelope );
		return envelope;
	}
	
	/* FOR TESTING ONLY */
	public void setSendToken( String sendToken ) {
		this.sendToken = sendToken;
	}

	/** 
	 * Set the state of an Envelope, return the state timestamp.
	 * <p>
	 * Subclasses should add broker-specific behavior.
	 * @throws IOException 
	 */
	protected String setState( Envelope envelope, State state ) throws IOException {
		
		SimpleEnvelope simpleEnvelope = (SimpleEnvelope) envelope;
		simpleEnvelope.setState( state );
		final String now = nowISO();
		
		switch( state ) {
		case Received:
			simpleEnvelope.setReceivedTimestamp( now );
			break;
		case Sent:
			simpleEnvelope.setSentTimestamp( now );
			break;
		case Expired:
			simpleEnvelope.setExpiredTimestamp( now );
			break;
		case Rejected:
			simpleEnvelope.setRejectedTimestamp( now );
			break;
		case Consumed:
			simpleEnvelope.setConsumedTimestamp( now );
			break;
		default:
			throw new RuntimeException( "Unsupported state: " + state );			
		}
		
		return now;
	}
	
	@Override
	public void setTokenFactory( TokenFactory factory ) {
		this.tokenFactory = factory;
		this.sendToken = factory.create();
	}

	@Override
	public void setAcceptedRoles( List<String> acceptedRoles ) {
		if( this.tokenFactory == null ) {
			throw new RuntimeException( "No token factory found" );
		}
		this.acceptedRoles = acceptedRoles;
	}
}
