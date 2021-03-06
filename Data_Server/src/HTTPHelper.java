import sun.net.www.protocol.http.HttpURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Helper class for parsing HTTP methods
 */
public class HTTPHelper {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());

  private final String HTTP_GET = "GET";
  private final long timeoutValue = 800;

  //HTTP Response Codes
  public final String HTTP200 = "HTTP/1.1 200 OK\n";
  public final String HTTP400 = "HTTP/1.1 400 Bad Request \r\n";
  public final String HTTP404 = "HTTP/1.1 404 Not Found \r\n";
  public final String HTTP405 = "HTTP/1.1 405 Method Not Allowed \r\n";

  private final String CONTENT_TYPE = "Content-Type: application/json \n";
  private String CONTENT_LENGTH = "Content-Length:";

  /**
   * Parses the first line of a request to determine if it is a GET Request
   * @return Whether the first line of a request specifies that it is a GET Request
   * */
  public boolean isHTTPGET(String headerLine){
    boolean isHTTPGet = false;
    if (headerLine != null){
      isHTTPGet = headerLine.contains(HTTP_GET);
    }
    return isHTTPGet;
  }

  /**
   * @return The resource that is trying to be accessed in the GET Request
   * */
  public String getResource(String getLine){
    String[] headerLine = getLine.split(" ");
    return headerLine[1];
  }

  /**
   * Parses the given URL Params into a maps
   * @param params The URL params to parse
   * @return Map of the URL params and their values
   * */
  public HashMap<String, String> parseURLParams(String params){
    HashMap<String, String> urlParamsMap = new HashMap<>();
    for (String param: params.split("&")){
      String[] paramKeyValue = param.split("=");
      if (paramKeyValue.length == 2){
        String key = paramKeyValue[0];
        String value = paramKeyValue[1];
        urlParamsMap.put(key, value);
      }else {
        return null;
      }
    }
    return urlParamsMap;
  }

  /**
   * Performs an HTTP GET on the given URL returning response
   *
   * @param urlString The URL to query
   * @return The string response
   */
  public String performHttpGet(String urlString) {
    String response = null;
    try {
      response = performHTTPMethod(urlString, HTTP_GET, 0);
    } catch (SocketTimeoutException e) {

    }
    return response;
  }

  /**
   * Performs an HTTP GET on the given URL returning response or false on timeout
   *
   * @param urlString The URL to query
   * @param timeoutValue The milliseconds to timeout after
   * @return The string response
   */
  public String performHttpGetWithTimeout(String urlString, int timeoutValue) throws SocketTimeoutException {
    return performHTTPMethod(urlString, HTTP_GET, timeoutValue);
  }

  /**
   * Private helper method that will perform the given HTTP Method on the given URL
   * Will be called by the other methods
   * @param urlString The URL for the HTTP Request
   * @param httpMethod The HTTP method to perform
   * */
  private String performHTTPMethod(String urlString, String httpMethod, int timeoutValue) throws SocketTimeoutException{
    //Builder for the response
    StringBuilder stringBuilder = new StringBuilder();

    HttpURLConnection httpURLConnection;
    try {
      logger.info("Sending request: " + urlString);
      URL url = new URL(urlString);
      httpURLConnection = (HttpURLConnection) url.openConnection();
      httpURLConnection.setRequestMethod(httpMethod);
      httpURLConnection.setRequestProperty("Content-Type", "application/json");

      if (timeoutValue > 0){
        httpURLConnection.setConnectTimeout(timeoutValue);
      }

      //Build String Response

      BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null){
        stringBuilder.append(line);
      }

    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      throw new SocketTimeoutException();
    }
    return stringBuilder.toString();
  }

  /**
   * Returns the full HTTP Response with full headers
   * */
  public String buildHTTPResponse(String json){
    return HTTP200 + CONTENT_TYPE + CONTENT_LENGTH + json.length() + "\n\r\n" + json;
  }
}
