package alma.obops.draws.messages.couchdb;

import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Standard configuration for a CouchDB connection: URL, username and password.
 * <ol>
 * <li>Consider runtime properties <code>archive.couchdb.connection</code>
 * (mandatory), <code>archive.couchdb.user</code> and
 * <code>archive.couchdb.passwd</code>; if none are found...*
 * <li>Consider properties defined in the ALMA standard Archive config file,
 * <em>$ACSDATA/config/archiveConfig.properties</em>:
 * <code>archive.couchdb.connection</code> (mandatory),
 * <code>archive.couchdb.user</code> and <code>archive.couchdb.passwd</code>; if
 * none are found...
 * <li>fall back on hardcoded constants {@link #COUCHDB_URL}
 * {@link COUCHDB_USERNAME} and {@link COUCHDB_PASSWORD}
 */

public class CouchDbConfig {

    public static final String URL_PROP = "archive.couchdb.connection";
    public static final String USERNAME_PROP = "archive.couchdb.user";
    public static final String PASSWORD_PROP = "archive.couchdb.passwd";

	public static final String COUCHDB_URL = "http://localhost:5984";
	public static final String COUCHDB_USERNAME = null;
	public static final String COUCHDB_PASSWORD = null;
    
    public static final String ACSDATA_ENV_VAR = "ACSDATA";
    public static final String ACSDATA_CONFIG_FILE = "config" + File.separator + "archiveConfig.properties";

    private String url;
    private String username;
    private String password;
    private String acsData;

    public CouchDbConfig() throws IOException {

        this.url = null;
        this.username = null;
        this.password = null;
        this.acsData = System.getenv( ACSDATA_ENV_VAR );
        // System.out.println( ">>> ACSDATA=" + this.acsData );

        // See if we have runtime properties: if so, we're done
        if( runtimeProperties() ) {
            return;
        }

        // See if we have properties in $ACSDATA/config/archiveConfig.properties: if so,
        // we're done
        if( almaProperties() ) {
            return;
        }

        // Fall back on hardcoded constants (probably a bad idea)
        this.url      = COUCHDB_URL;
        this.username = COUCHDB_USERNAME;
        this.password = COUCHDB_PASSWORD;
    }

    public CouchDbConfig( String url, String username, String password ) {
        this.url      = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    /** See if any runtime props were given */
    private boolean findProperties( Properties properties ) {
        url = (String) properties.get(URL_PROP);
        if( isEmpty( url )) {
            return false;
        }
        
        username = (String) properties.get( USERNAME_PROP );
        if( isEmpty( username )) {
            return true;
        }
        
        password = (String) properties.get( PASSWORD_PROP );
        if( isEmpty( password )) {
            return true;
        }

        return true;
    }

    /** See if any runtime props were given */
    private boolean runtimeProperties() {
        Properties sysprop = System.getProperties();
        return findProperties( sysprop );
    }

    /**
     * See if we have properties in the ALMA standard Archive config file
     * 
     * @throws IOException
     */
    private boolean almaProperties() throws IOException {
        if( acsData == null ) {
            return false;
        }

        // Do we have that properties file?
        String pathname = this.acsData + File.separator + ACSDATA_CONFIG_FILE;
        File archivePropsFile = new File( pathname );
        if( ! ( archivePropsFile.exists() && archivePropsFile.canRead() )) {
           return false;
        }

        // System.out.println( ">>> reading properties from " + pathname );
        Properties almaProps = new Properties();
        almaProps.load( new FileInputStream( pathname ));

        return findProperties( almaProps );
    }

    public boolean isEmpty( String s ) {
        return s == null || s.length() == 0;
    }

    public static void main( String[] args ) throws IOException {
        CouchDbConfig config = new CouchDbConfig();
        System.out.println( "url: " + config.getUrl() );
        System.out.println( "username: " + config.getUsername() );
        System.out.println( "password: " + config.getPassword() );
    }
}