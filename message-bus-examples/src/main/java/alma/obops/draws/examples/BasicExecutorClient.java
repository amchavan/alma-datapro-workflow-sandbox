package alma.obops.draws.examples;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import alma.obops.draws.examples.BasicExecutor.DatetimeRequest;
import alma.obops.draws.examples.BasicExecutor.DatetimeResponse;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;

/**
 * Example client for basic executor defined in {@link #BasicExecutor}
 */
public class BasicExecutorClient implements Runnable {

	@Autowired
	private MessageBroker broker;

	@Override
	public void run() {
	
		MessageQueue queue = broker.messageQueue( BasicExecutor.DATETIME_QUEUE );
		MessageConsumer consumer = (message) -> {
			System.out.println(((DatetimeResponse) message).datetime);
		};
		
		ExecutorClient client = new ExecutorClient( queue, consumer );
		DatetimeRequest request = new DatetimeRequest( "Etc/GMT" );

		try {
			client.call( request );
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		}
		
		System.exit( 0 );
	}
}
