package alma.obops.draws.messages;

import java.io.IOException;
import java.util.List;

/**
 * @author mchavan 10-Oct-2018
 */
public abstract class AbstractMessageBroker implements MessageBroker {

	public AbstractMessageBroker() {
		// empty
	}

	@Override
	public void deleteQueue(MessageQueue queue) {
		// no-op
	}

	@Override
	public Thread listenInThread( MessageQueue queue, MessageConsumer consumer, int timeout, boolean justOne) {
		
		Runnable receiver = () -> {	
			try {
				this.listen( queue, consumer, timeout, justOne );
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
			Envelope ret = sendOne( queue, message, expireTime );		// No, just send this message
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
				ret = sendOne( memberQueue, message, expireTime );
			}
			return ret;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
}
