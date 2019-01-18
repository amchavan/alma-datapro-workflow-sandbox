package alma.adapt.examples.common;

/**
 * Common utilities
 */
public class Utils {

	/**
	 * Process command line arguments of the form
	 * <code>&lt;argname&gt;=&lt;argvalue&gt;</code>, for instance
	 * <code>pathname=/tmp/q.txt</code><br>
	 * Rudimentary implementation!
	 * 
	 * @return The value, or <code>null</code>.
	 */
	public static String getCommandLineArg( String argName, String... args ) {
		if( args.length > 0 ) {
			for( String arg : args ) {
				if( arg.startsWith( argName )) {
					String[] t = arg.split( "=" );
					return t[1];
				}
			}
		}
		return null;
	}

	/** Thread.sleep() with no checked exceptions */
	public static void sleep( int delay ) {
		try {
			Thread.sleep( delay * 1000 );
		} 
		catch( InterruptedException e ) {
			throw new RuntimeException( e );
		}
	}
}
