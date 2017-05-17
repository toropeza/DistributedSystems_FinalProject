import DataModel.ServerInfo;
import ResponseModels.SuccessResponse;
import com.google.gson.Gson;
import util.JSONServerConfigModel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

/**
 * Message server that asynchronously handles various connections
 */
public class WebServer {

  static final Logger logger = Logger.getLogger(WebServer.class.getName());


  final static String portParam = "-port";
  final static String dataServerIPParam = "-dsip";
  final static String dataServerPortParam = "-dsport";

  private static String configFilename;

  public static void main(String[] args) {
    Gson gson = new Gson();

    configFilename = args[0];
    try {
      BufferedReader reader = new BufferedReader(new FileReader(configFilename));
      String jsonConfig = readFile(reader);
      JSONServerConfigModel config = gson.fromJson(jsonConfig, JSONServerConfigModel.class);
      int port = config.getPort();
      List<ServerInfo> dataServers = config.getDataServers();
      WebServerAPI webServerAPI = new WebServerAPI(dataServers);
      logger.info("Web Server Running on Port " + port);

      try (ServerSocket serverSocket = new ServerSocket(port);) {
        //repeatedly wait for connections
        while (true){
          Socket socket = serverSocket.accept();
          webServerAPI.parseAPIRequest(socket);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  /**
   * Returns the contents of the given file
   * @param reader The reader for the file
   * @return The file's contents
   * @throws IOException
   */
  public static String readFile(BufferedReader reader) throws IOException {
    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
    return builder.toString();
  }
}
