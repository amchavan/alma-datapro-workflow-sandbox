package alma.obops.draws.messages.rabbitmq;

import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;

/**
 * @author mchavan  11-Oct-2018
 */
public class RabbitMqMessageQueue extends MessageQueue {

	private String rmqQueueName;

	public RabbitMqMessageQueue( String queueName, MessageBroker broker, String rmqQueueName ) {
		super( queueName, broker );
		this.rmqQueueName = rmqQueueName;
	}

	public String getRmqQueueName() {
		return rmqQueueName;
	}
}
