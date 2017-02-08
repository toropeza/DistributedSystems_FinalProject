import java.util.HashMap;

/**
 * Helper class for parsing HTTP methods
 */
public class HTTPHelper {

  private final String HTTP_GET = "GET";

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

}
