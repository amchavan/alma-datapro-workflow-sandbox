package alma.icd.adapt.messagebus;

/** Defines procedures that consume a request message and return a reply message */
public interface RequestProcessor {
	public ResponseMessage process( RequestMessage message );
}