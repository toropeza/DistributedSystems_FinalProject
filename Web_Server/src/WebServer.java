import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Message server that asynchronously handles various connections
 */
public class WebServer {

  static final Logger logger = LogManager.getLogger(WebServer.class);

  final static String portParam = "-port";
  final static String dataServerIPParam = "-dsip";
  final static String dataServerPortParam = "-dsport";

  public static void main(String[] args) {
    ArgumentParser argumentParser = new ArgumentParser(args);
    if (argumentParser.containsArgument(portParam) && argumentParser.containsArgument(dataServerPortParam) && argumentParser.containsArgument(dataServerIPParam)){
      String port = argumentParser.getValueForArgument(portParam);
      String dsPort = argumentParser.getValueForArgument(dataServerPortParam);
      String dsIP = argumentParser.getValueForArgument(dataServerIPParam);
      WebServerAPI webServerAPI = new WebServerAPI(dsPort, dsIP);
      logger.info("Web Server Running on Port " + port);

      try (ServerSocket serverSocket = new ServerSocket(Integer.valueOf(port));) {
        //repeatedly wait for connections
        while (true){
          Socket socket = serverSocket.accept();
          webServerAPI.parseAPIRequest(socket);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }else {
      System.out.println("Invalid arguments");
    }
  }
}
