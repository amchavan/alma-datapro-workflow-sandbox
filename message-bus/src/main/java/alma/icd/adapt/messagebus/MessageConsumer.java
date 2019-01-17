package alma.icd.adapt.messagebus;

/** Defines procedures that consume a message */
public interface MessageConsumer {
	public void consume( Message message );
}