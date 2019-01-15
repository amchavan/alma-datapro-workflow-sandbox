package alma.obops.draws.dashboard.persistence;

import static alma.obops.draws.dashboard.persistence.HttpUtils.*;

import java.io.IOException;
import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import com.fasterxml.jackson.databind.ObjectMapper;

import alma.obops.draws.dashboard.persistence.envelope.Record;

/**
 * An interface to CouchDB
 * @author mchavan
 */
public class CouchDbConnection implements DbConnection {

	private String baseURL;
	private Header authHeader;

	/** Constructor: create an interface to a non-secured CouchDB instance */
	public CouchDbConnection( String baseURL ) {
		this( baseURL, null, null );
	}
	
//	/** Constructor: create an interface to a CouchDB instance */
//	public CouchDbConnection( CouchDbConfig config ) {
//		this( config.getUrl(), config.getUsername(), config.getPassword() );
//	}
	
	/** Constructor: create an interface to a CouchDB instance */
	public CouchDbConnection( String baseURL, String username, String password ) {
		
		this.baseURL = baseURL;
		this.authHeader = null;
		
		if( username != null && password != null ) {
			String auth = username + ":" + password;
			String encodedAuth = Base64.getEncoder().encodeToString( auth.getBytes() );
			this.authHeader = new BasicHeader( "Authorization", "Basic " + encodedAuth );
		}
	}
	
	/** Create a new database */
	public void dbCreate( String dbName ) throws IOException {

		String url = baseURL + "/" + dbName;
		org.apache.http.HttpResponse response = httpPut( url, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
//		System.out.println( ">>> create(): " + status );

		if( status == 412 /* was already there */ || status == 201 /* really created */  ) {
			return;
		}
		
		String fmt = "create('%s') failed: status=%d, '%s'";
		String msg = String.format(fmt, dbName, status, readBody( response ));
		throw new RuntimeException( msg );
	}
	
	/** Delete a database */
	public void dbDelete( String dbName ) throws IOException {
		String url = baseURL + "/" + dbName;

		HttpResponse response = httpDelete( url, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
//		System.out.println( ">>> delete(): " + status );
		
//		System.out.println( "Delete: " + status );
		if( status == 404 /* not found */ || status == 200 /* really deleted */  ) {
			return;
		}
		
		String fmt = "delete('%s') failed: status=%d, '%s'";
		String msg = String.format(fmt, dbName, status, readBody( response ));
		throw new RuntimeException( msg );
	}

	/**
	 * @return <code>true</code> if the input database exists, <code>false</code>
	 *         otherwise
	 * @throws IOException
	 */
	public boolean dbExists( String dbName ) throws IOException {
		String url = baseURL + "/" + dbName;
		HttpResponse response = httpGet( url, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
		return status == 200;
	}

	/**
	 * Delete a record from a database.
	 * 
	 * @param dbName
	 *            Name of the database
	 * @param record
	 *            Record to delete (you may have to retrieve the record before
	 *            deleting or you risk a status 409, "document update conflict" )
	 * @throws IOException 
	 */
	public void delete( String dbName, Record record ) throws IOException {
		delete( dbName, record.getId(), record.getVersion() );
	}
	
	/**
	 * Delete a record from a database.
	 * 
	 * @param dbName
	 *            Name of the database
	 * @param id
	 *            Object/record ID
	 * @param version
	 *            Current version of the record (you may have to retrieve the record
	 *            before deleting or you risk a status 409, "document update conflict" )
	 * @throws IOException 
	 */
	public void delete( String dbName, String id, String version ) throws IOException {
		
		if( isEmpty( dbName ) || isEmpty( id ) || isEmpty( version ) ) {
			throw new IllegalArgumentException( "Null or empty arg" );
		}
		
		String url = baseURL + "/" + dbName + "/" +  urlEncode( id.toString() ) + "?rev=" +  urlEncode( version );

		HttpResponse response = httpDelete( url, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
//		System.out.println( ">>> delete(): " + status );
		
//		System.out.println( "Delete: " + status );
		if( status == 404 /* not found */ || status == 200 /* really deleted */  ) {
			return;
		}
		
		String fmt = "delete('%s','%s','%s') failed: status=%d, '%s'";
		String msg = String.format( fmt, dbName, id, version, status, readBody( response ));
		throw new RuntimeException( msg );
	}

	/**
	 * Search for documents; selector includes the query parameters.
	 * 
	 * @param dbName	    Name of the database
	 * @param arrayClass	Class of the objects *array* that should be returned
	 * @param query			A JSON selector like
	 * <pre>
{
	"selector": {
		"state": "PartiallyObserved"
	}
}
	 * </pre>
	 *		
	 * See also http://docs.couchdb.org/en/2.1.1/api/database/find.html
	 * 
	 * @return A possibly empty array of documents
	 */
	public <T extends Object> T find( String dbName, Class<T> arrayClass, String query ) throws IOException {
	
		if( isEmpty( dbName ) || arrayClass == null || isEmpty( query )) {
			throw new IllegalArgumentException( "Null or empty arg" );
		}
		
		String url = baseURL + "/" + dbName + "/_find"; 
		HttpResponse response = httpPost( url, query, ContentType.APPLICATION_JSON, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
		if( status == 200 ) {

			String json = readBody( response );
			ObjectMapper objectMapper = new ObjectMapper();
			com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(json);
			String rows = jsonNode.get( "docs" ).toString();
			T objs = objectMapper.readValue( rows, arrayClass );
			return arrayClass.cast( objs );
		}
		
		String fmt = "find('%s','%s') failed: status=%d, '%s'";
		String msg = String.format( fmt, dbName, query, status, readBody( response ));
		throw new RuntimeException( msg );
	}	
	
	/**
	 * Query a database and return all records.
	 * 
	 * @param dbName		Name of the database
	 * @param arrayClass	Class of the objects *array* that should be returned
	 * 
	 * @return The object, if one is found, <code>null</code> otherwise
	 */
	public <T extends Object> T findAll( String dbName, Class<T> arrayClass ) throws IOException {
		String selector = "{'selector': {'_id': {'$gt': null }}}".replaceAll( "'", "\"" );
		return find( dbName, arrayClass, selector );
	}

	/**
	 * Query a database by ID (unique key).
	 * @param <T>
	 * 
	 * @param dbName	Name of the database
	 * @param clasz		Class of the object that should be returned
	 * @param id		Object/record ID
	 * 
	 * @return The object, if one is found, <code>null</code> otherwise
	 */
	public <T extends Object> T findOne( String dbName, Class<T> clasz, String id ) throws IOException {

		if( isEmpty( dbName ) || clasz == null || isEmpty( id ) ) {
			throw new IllegalArgumentException( "Null or empty arg" );
		}
		
		String url = baseURL + "/" + dbName + "/" + urlEncode( id.toString() );
		HttpResponse response = httpGet( url, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
//		System.out.println( ">>> findOne(): " + status );

		if( status == 404 ) {
			return null;
		}
		
		if( status != 200 ) {
			String fmt = ">>>> findOne('%s','%s') failed: status=%d, '%s'";
			String msg = String.format( fmt, dbName, id, status, response.getEntity().toString() );
			throw new RuntimeException( msg );
		}
	
		String json = readBody( response );
		ObjectMapper objectMapper = new ObjectMapper();
		T o = objectMapper.readValue( json, clasz );

		return o;
	}

	/**
	 * @return The current version of the input record
	 * @param dbName	Name of the database
	 * @param rec		Object that should be refreshed
	 */
	@SuppressWarnings("unchecked")
	public <T extends Record> T findOne( String dbName, Record rec ) throws IOException {
		return (T) findOne( dbName, rec.getClass(), rec.getId() );
	}

	private boolean isEmpty( String s ) {
		return s == null || s.length() == 0;
	}

	/**
	 * Save or update a record
	 * 
	 * @param dbName	Name of the database
	 * @param record	Object to be saved
	 */
	public void save( String dbName, Record record ) throws IOException {
		
		if( isEmpty( dbName ) || record == null ) {
			throw new IllegalArgumentException( "Null or empty arg" );
		}

		Record found = findOne( dbName, record.getClass(), record.getId() );
		if( found != null ) {
			// Record exists already, need to retrieve its current version or
			// we won't be able to update it
			String version = found.getVersion();
			record.setVersion( version );
		}
		
		String id = urlEncode( record.getId().toString() );
		String url = baseURL + "/" + dbName + "/" + id;
		
		// Write our record to the database
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString( record );

		HttpResponse response = httpPut( url, json, this.authHeader );
		int status = response.getStatusLine().getStatusCode();
//		System.out.println( ">>> delete(): " + status );

		if( status == 201 /* created */ ) {
			return;
		}
		
		String fmt = "save('%s',%s) failed: status=%d, '%s'";
		String msg = String.format(fmt, dbName, record, status, readBody( response ));
		throw new RuntimeException( msg );
	}
}
