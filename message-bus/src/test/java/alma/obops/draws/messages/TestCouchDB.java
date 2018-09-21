package alma.obops.draws.messages;

import static alma.obops.draws.messages.TestUtils.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import alma.obops.draws.messages.couchdb.CouchDbConnection;


public class TestCouchDB {
	
	private CouchDbConnection db;
	private final TestUtils.TestRecord jimi = new TestUtils.TestRecord( "Jimi Hendrix", 28, false );

	@Before
	public void setUp() throws IOException {
		this.db = new CouchDbConnection( COUCHDB_URL, null, null );
		db.dbDelete( TEST_DB_NAME );
		db.dbCreate( TEST_DB_NAME );
	}
	
	@Test
	public void dbShouldBeThere() throws IOException {
		assertTrue( "Database '" + TEST_DB_NAME + "' not found", db.dbExists( TEST_DB_NAME ) );
	}
	
	@Test
	public void find() throws IOException {
		
		TestUtils.TestRecord tr1 = jimi;											// Jimi is dead, but...
		TestUtils.TestRecord tr2 = new TestUtils.TestRecord( "Brian May", 71, true ); 		// ...May is still alive...
		TestUtils.TestRecord tr3 = new TestUtils.TestRecord( "John Petrucci", 51, true ); 	// ...and so is Petrucci
		
		db.save( TEST_DB_NAME, tr1 );
		db.save( TEST_DB_NAME, tr2 );
		db.save( TEST_DB_NAME, tr3 );
		
		String query1 = "{'selector': {'alive': true } }";
		TestUtils.TestRecord[] alive = (TestUtils.TestRecord[]) db.find( TEST_DB_NAME, TestUtils.TestRecord[].class, query1.replaceAll( "'", "\"" ) );
		assertNotNull( alive );
		assertEquals( 2, alive.length );
		
		String query2 = "{'selector': {'alive': false } }";
		TestUtils.TestRecord[] dead = (TestUtils.TestRecord[]) db.find( TEST_DB_NAME, TestUtils.TestRecord[].class, query2.replaceAll( "'", "\"" ) );
		assertNotNull( dead );
		assertEquals( 1, dead.length );
		assertEquals( "Jimi Hendrix", dead[0].name );
		
		String query3 = "{'selector': {'name': {'$regex': '^John'}}}";
		TestUtils.TestRecord[] john = (TestUtils.TestRecord[]) db.find( TEST_DB_NAME, TestUtils.TestRecord[].class, query3.replaceAll( "'", "\"" ) );
		assertNotNull( john );
		assertEquals( 1, john.length );
		assertEquals( "John Petrucci", john[0].name );
	}
	
	@Test
	public void findAll() throws IOException {
		
		TestUtils.TestRecord tr1 = jimi;											// Jimi is dead, but...
		TestUtils.TestRecord tr2 = new TestUtils.TestRecord( "Brian May", 71, true ); 		// ...May is still alive...
		TestUtils.TestRecord tr3 = new TestUtils.TestRecord( "John Petrucci", 51, true ); 	// ...and so is Petrucci
		
		db.save( TEST_DB_NAME, tr1 );
		db.save( TEST_DB_NAME, tr2 );
		db.save( TEST_DB_NAME, tr3 );
		
		TestUtils.TestRecord[] all = (TestUtils.TestRecord[]) db.findAll( TEST_DB_NAME, TestUtils.TestRecord[].class );
		assertNotNull( all );
		assertEquals( 3, all.length );
	}

	@Test
	public void findOne() throws IOException {
		db.save( TEST_DB_NAME, jimi );

		TestUtils.TestRecord ret1 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		TestUtils.TestRecord ret2 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi );
		assertEquals( ret1, ret2 );
	}

	@Test
	public void nullOrEmptyArgs() throws IOException {
		try {
			db.findOne( null, null, null );
			fail( "Expected IllegalArgumentException" );
			db.findOne( "", null, null );
			fail( "Expected IllegalArgumentException" );
			db.findOne( TEST_DB_NAME, null, null );
			fail( "Expected IllegalArgumentException" );
			db.findOne( TEST_DB_NAME, jimi.getClass(), null );
			fail( "Expected IllegalArgumentException" );
			db.findOne( TEST_DB_NAME, jimi.getClass(), "" );
			fail( "Expected IllegalArgumentException" );
		}
		catch( IllegalArgumentException ignored ) {
			// no-op, expected
		}
	}
	
	@Test
	public void saveThenDelete() throws IOException {
		db.save( TEST_DB_NAME, jimi );
		TestUtils.TestRecord ret1 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertNotNull( ret1 );
		
		db.delete( TEST_DB_NAME, ret1.getId(), ret1.getVersion() );
		TestUtils.TestRecord ret2 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertNull( ret2 );
	}
	
	@Test
	public void saveThenDeleteTwice() throws IOException {
		db.save( TEST_DB_NAME, jimi );
		TestUtils.TestRecord ret1 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		db.delete( TEST_DB_NAME, ret1.getId(), ret1.getVersion() );
		db.delete( TEST_DB_NAME, ret1.getId(), ret1.getVersion() );
	}

	@Test
	public void saveThenDeleteWithRecord() throws IOException {
		db.save( TEST_DB_NAME, jimi );
		TestUtils.TestRecord ret1 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertNotNull( ret1 );
		
		db.delete( TEST_DB_NAME, ret1 );
		TestUtils.TestRecord ret2 = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertNull( ret2 );
	}

	@Test
	public void saveThenFind() throws IOException {
		db.save( TEST_DB_NAME, jimi );
		
		Object ret1 = db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertEquals( TestUtils.TestRecord.class, ret1.getClass() );
		TestUtils.TestRecord out = (TestUtils.TestRecord) ret1;
		System.out.println( out );
		assertEquals( 28, out.age );

		Object ret2 = db.findOne( TEST_DB_NAME, jimi.getClass(), "-1" );	// should fail and return null
		assertNull( ret2 );
	}

	@Test
	public void saveThenUpdate() throws IOException {
		db.save( TEST_DB_NAME, jimi );
		
		jimi.age = 29;	// poor Jimi never got to be that old, actually
		db.save( TEST_DB_NAME, jimi );
		
		TestUtils.TestRecord out = (TestUtils.TestRecord) db.findOne( TEST_DB_NAME, jimi.getClass(), jimi.getId() );
		assertEquals( 29, out.age );
	}

	@Test
	public void simpleSave() throws IOException {
		db.save( TEST_DB_NAME, jimi );
	}
}
