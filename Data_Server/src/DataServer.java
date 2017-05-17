import DataModel.ServerInfo;
import Paxos.PaxosManager;
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
public class DataServer {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());
  static final Gson gson = new Gson();

  private static String configFilename;

  public static void main(String[] args) {
    try {
      if (args.length == 0) {
        logger.warning("Please provide the config filename");
        System.exit(1);
      }
      configFilename = args[0];
      BufferedReader reader = new BufferedReader(new FileReader(configFilename));
      String jsonConfig = readFile(reader);
      JSONServerConfigModel config = gson.fromJson(jsonConfig, JSONServerConfigModel.class);
      int port = config.getPort();
      List<ServerInfo> otherDataServers = config.getDataServers();
      DataServerAPI dataServerAPI = new DataServerAPI(port, otherDataServers, config.getTest(), config.getStartSequenceNum());
      try (ServerSocket serverSocket = new ServerSocket(port);) {

        logger.info("Data Server Running on port " + port);
        //repeatedly wait for connections
        while (true) {
          Socket socket = serverSocket.accept();
          logger.info("Accepted Request");
          dataServerAPI.parseAPIRequest(socket);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      logger.warning("Cannot find config file: " + configFilename);
    } catch (IOException e) {
      logger.warning("Cannot read config file");
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
