package alma.obops.draws.messages.examples;

import java.lang.reflect.Method;

/**
 * Central executor for the examples in this package
 * @author mchavan, 19-Sep-2018
 */
public class ExampleDispatcher {

	/**
	 * Usage<br>
	 * <code>java -jar .....jar &lt;example&gt;</code><br>
	 * where <i>example</i> is the simple class name of one of the examples, for
	 * instance {@link BasicSender}.
	 */
	public static void main(String[] args) throws Exception {
		String target = args[0];
		String pckg = ExampleDispatcher.class.getPackage().getName();
		Class<?> c = Class.forName( pckg + "." + target ); 
		Method m = c.getMethod( "main", String[].class );
		m.invoke( null, (Object[]) new String[1] );
	}
}
