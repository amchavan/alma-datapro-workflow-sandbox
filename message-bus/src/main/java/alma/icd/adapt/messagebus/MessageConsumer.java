package alma.obops.draws.messages;

/** Defines procedures that consume a message */
public interface MessageConsumer {
	public void consume( Message message );
}