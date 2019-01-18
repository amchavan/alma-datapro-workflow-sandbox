package alma.icd.adapt.messagebus;

public interface Record {

	public String getId() ;
	public String getVersion();
	public void setVersion( String version );
}
