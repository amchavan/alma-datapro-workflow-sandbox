package alma.obops.draws.messages;

/** Defines procedures that consume a request message and return a reply message */
public interface RequestProcessor {
	public Message process( RequestMessage message );
}