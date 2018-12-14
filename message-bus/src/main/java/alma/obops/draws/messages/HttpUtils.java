package alma.obops.draws.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	/**
	 * Perform a GET with no request headers.
	 * 
	 * @param url URL of the resource
	 */
	public static String httpGetString( String url ) {
		return httpGetString(url, new Header[0] );
	}

	/**
	 * Perform a GET, convert the resulting JSON to a keyword/value map
	 * 
	 * @param url     URL of the resource, it is expected to be returning a simple
	 *                JSON object
	 *                
	 * @param headers Request headers
	 */
	public static String httpGetString( String url, Header[] headers ) {
		HttpResponse response = null;
		try {
			response = httpGet( url, headers );
		
			StatusLine statusLine = response.getStatusLine();
		 	int statusCode = statusLine.getStatusCode();
			if( statusCode != 200 ) {
				throw new RuntimeException( "HTTP response status: "
						+ statusCode
						+ ", "
						+ statusLine.getReasonPhrase() );
			}
			
			// The body should be a JSON structure
			String body = readBody( response );
			return body;
		} 
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
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

	/**
	 * Read the body of an {@link HttpResponse} into a map of keyword/value pairs
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static Map<String,String> readBodyAsMap( HttpResponse response ) 
				throws JsonParseException, JsonMappingException, IOException {
		
		// see https://stackoverflow.com/questions/2525042/how-to-convert-a-json-string-to-a-mapstring-string-with-jackson-json
		TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {};
		ObjectMapper mapper = new ObjectMapper(); 
	    String json = readBody( response );
	    HashMap<String,String> map = mapper.readValue( json, typeRef );
		return map;
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

	/**
	 * Perform a GET, convert the resulting JSON to a keyword/value map. <br>
	 * No request headers.
	 * 
	 * @param url URL of the resource, it is expected to be returning a simple JSON
	 *            object
	 */
	public static Map<String, String> httpGetMap( String url ) {
		return httpGetMap(url, new Header[0] );
	}

	/**
	 * Perform a GET, convert the resulting JSON to a keyword/value map
	 * 
	 * @param url     URL of the resource, it is expected to be returning a simple
	 *                JSON object
	 *                
	 * @param headers Request headers
	 */
	public static Map<String, String> httpGetMap( String url, Header[] headers ) {
		HttpResponse response = null;
		try {
			response = httpGet( url, headers );
		
			StatusLine statusLine = response.getStatusLine();
		 	int statusCode = statusLine.getStatusCode();
			if( statusCode != 200 ) {
				throw new RuntimeException( "HTTP response status: "
						+ statusCode
						+ ", "
						+ statusLine.getReasonPhrase() );
			}
			
			// The body should be a JSON structure
			Map<String, String> body = readBodyAsMap( response );
			return body;
		} 
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

}
