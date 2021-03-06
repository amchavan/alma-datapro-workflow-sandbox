package alma.icd.adapt.messagebus;

/**
 * All user messages implement this interface.
 * 
 * @author mchavan, 12-Sep-2018
 */
public interface Message {
	 public Envelope getEnvelope();
	 public void setEnvelope( Envelope envelope );
}
