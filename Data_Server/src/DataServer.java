import DataModel.DataServerInfo;
import DataModel.WebServerInfo;
import com.google.gson.Gson;
import util.JSONServerConfigModel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Message server that asynchronously handles various connections
 */
public class DataServer {

  static final Logger logger = Logger.getLogger(DataServer.class.getName());
  static final Gson gson = new Gson();
  static final String configFilename = "DATA_SERVER_CONFIG.json";

  static final String primaryServerType = "primary";
  static final String secondaryServerType = "secondary";

  public static void main(String[] args) {
    try {
      if (args.length == 0) {
        logger.warning("Please provide the config filename");
        System.exit(1);
      }
      BufferedReader reader = new BufferedReader(new FileReader(args[0]));
      String jsonConfig = readFile(reader);
      JSONServerConfigModel config = gson.fromJson(jsonConfig, JSONServerConfigModel.class);

      if (config.isTest()){
        logger.info("Starting server as test server");
        DataServerAPI.testing = true;
      }
      int port = config.getPort();
      String serverType = config.getServer_type();
      if (serverType != null) {
        DataServerAPI dataServerAPI = new DataServerAPI(port, config.isTest());

        try (ServerSocket serverSocket = new ServerSocket(port);) {
          if (serverType.equals(primaryServerType)) {
            DataServerAPI.setPrimary();
            List<WebServerInfo> webServerIps = config.getWebservers();
            if (webServerIps != null) {
              dataServerAPI.setWebServers(webServerIps);
            } else {
              logger.warning("No web servers found in config file, running lone data server");
            }
          } else if (serverType.equals(secondaryServerType)) {
            DataServerInfo primaryInfo = new DataServerInfo();
            primaryInfo.setPort(Integer.valueOf(config.getPrimaryPort()));
            primaryInfo.setIp(config.getPrimaryIp());
            DataServerAPI.primaryInfo = primaryInfo;
            dataServerAPI.setSecondary(config.getPrimaryIp(), Integer.valueOf(config.getPrimaryPort()));
            dataServerAPI.queryPrimaryForData(port, config.getPrimaryIp(), Integer.valueOf(config.getPrimaryPort()), config.isTest());
          }

          logger.info("Data Server Running");
          //repeatedly wait for connections
          while (true) {
            Socket socket = serverSocket.accept();
            logger.info("Accepted Request");
            dataServerAPI.parseAPIRequest(socket);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
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
