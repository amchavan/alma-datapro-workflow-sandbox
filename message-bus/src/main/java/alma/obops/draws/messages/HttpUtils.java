package alma.obops.draws.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

/**
 * A collection of methods to help with {@link  HttpClient}
 * @author mchavan
 *
 */
public class HttpUtils {

	/**
	 * Populate a {@link org.apache.http.client.fluent.Request} with some headers,
	 * than execute it and return the {@link HttpResponse}
	 * 
	 * @throws IOException
	 */
	public static HttpResponse execute( Request request, Header... headers ) throws IOException {
		for( Header header: headers ) {
			if( header != null ) {
				request.addHeader( header );
			}
		}
		return request.execute().returnResponse();
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Delete} */
	public static HttpResponse httpDelete( String url, Header... headers ) throws IOException {
		Request request = Request.Delete( url );
		return execute( request, headers );
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Get} */
	public static HttpResponse httpGet( String url, Header... headers ) throws IOException {
		Request request = Request.Get( url );
		return execute( request, headers );
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Post} */	
	public static HttpResponse httpPost( String url, String body, ContentType contentType, Header... headers ) throws IOException {
		Request request = Request.Post( url );
		request.bodyString( body, contentType );
		return execute( request, headers );
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Put} */	
	public static HttpResponse httpPut( String url, Header... headers ) throws IOException {
		return httpPut( url, null, headers );
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Put} */	
	public static HttpResponse httpPut( String url, String body, ContentType contentType, Header... headers ) throws IOException {
		Request request = Request.Put( url );
		if( body != null ) {
			request.bodyString( body, contentType );
		}
		return execute( request, headers );
	}

	/** Wrapper around {@link org.apache.http.client.fluent.Request.Put} */	
	public static HttpResponse httpPut( String url, String body, Header... headers ) throws IOException {
		return httpPut( url, body, ContentType.TEXT_HTML, headers );
	}

	/** Read the body of an {@link HttpResponse} into a string */
	public static String readBody( HttpResponse response ) {
		try {
			InputStream stream = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader( new InputStreamReader( stream ));
			StringBuilder sb = new StringBuilder();
			
			while( true ) {
				
				String line = reader.readLine();
				if( line == null ) {
					break;
				}
				if( sb.length() > 0 ) {
					sb.append( '\n' );
				}
				sb.append( line );
			}
				
			reader.close();
			return sb.toString();
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/** Encode a string so that it can be used within a URL */
	public static String urlEncode(String value) {
		
		String ret = null;
		try {
			ret = URLEncoder.encode(value, StandardCharsets.UTF_8.toString() );
		} 
		catch (UnsupportedEncodingException e) {
			// should never happen
		}
		return ret;
	}

}
