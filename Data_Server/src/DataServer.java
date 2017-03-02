import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Message server that asynchronously handles various connections
 */
public class DataServer {

  static final Logger logger = LogManager.getLogger(DataServer.class);

  public static void main(String[] args) {
    if (args.length == 1){
      int port = Integer.valueOf(args[0]);
      DataServerAPI dataServerAPI = new DataServerAPI();
      logger.info("Data Server Running on port " + port);

      try (ServerSocket serverSocket = new ServerSocket(port);) {

        //repeatedly wait for connections
        while (true){
          Socket socket = serverSocket.accept();
          logger.info("Accepted Request");
          dataServerAPI.parseAPIRequest(socket);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }else {
      System.out.println("Please provide the port to run the Server on");
    }
  }
}
