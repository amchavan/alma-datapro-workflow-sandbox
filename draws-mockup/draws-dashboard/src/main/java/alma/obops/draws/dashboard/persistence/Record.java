package alma.obops.draws.dashboard.persistence;

public interface Record {

	public String getId() ;
	public String getVersion();
	public void setVersion( String version );
}
