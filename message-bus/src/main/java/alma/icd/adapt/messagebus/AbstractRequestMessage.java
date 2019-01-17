package alma.icd.adapt.messagebus;

/**
 * @author mchavan 21-Dec-2018
 */
public abstract class AbstractRequestMessage extends AbstractMessage implements RequestMessage {

	private String responseQueueName;
	
	@Override
	public String getResponseQueueName() {
		return responseQueueName;
	}

	@Override
	public void setResponseQueueName(String responseQueueName) {
		this.responseQueueName = responseQueueName;
	}

}
