package alma.obops.draws.dashboard.persistence;

import java.io.IOException;

/**
 * Describes a generic interface to a database of {@link #Record}s
 * 
 * @author mchavan
 *
 */
public interface DbConnection {
	
	/** Create a database */
	public void dbCreate( String dbName ) throws IOException;
	
	/** Delete a database */
	public void dbDelete( String dbName ) throws IOException;

	/**
	 * @return <code>true</code> if the input database exists, <code>false</code>
	 *         otherwise
	 * @throws IOException
	 */
	public boolean dbExists( String dbName ) throws IOException;

	/**
	 * Delete a record from a database.
	 * 
	 * @param dbName  Name of the database
	 * @param record  Record to delete
	 * @throws IOException 
	 */
	public void delete( String dbName, Record record ) throws IOException;
	
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
	public void delete( String dbName, String id, String version ) throws IOException;

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
	public <T extends Object> T find( String dbName, Class<T> arrayClass, String query ) throws IOException;
	
	/**
	 * Query a database and return all records.
	 * 
	 * @param dbName		Name of the database
	 * @param arrayClass	Class of the objects *array* that should be returned
	 * 
	 * @return The object, if one is found, <code>null</code> otherwise
	 */
	public <T extends Object> T findAll( String dbName, Class<T> arrayClass ) throws IOException;

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
	public <T extends Object> T findOne( String dbName, Class<T> clasz, String id ) throws IOException;

	/**
	 * @return The current version of the input record
	 * @param dbName	Name of the database
	 * @param rec		Object that should be refreshed
	 */
	public <T extends Record> T findOne( String dbName, Record rec ) throws IOException;

	/**
	 * Save or update a record
	 * 
	 * @param dbName	Name of the database
	 * @param record	Object to be saved
	 */
	public void save( String dbName, Record record ) throws IOException;
}
