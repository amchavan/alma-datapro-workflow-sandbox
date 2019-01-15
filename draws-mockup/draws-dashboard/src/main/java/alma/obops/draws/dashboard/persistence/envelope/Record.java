package alma.obops.draws.dashboard.persistence.envelope;

public interface Record {

	public String getId() ;
	public String getVersion();
	public void setVersion( String version );
}
