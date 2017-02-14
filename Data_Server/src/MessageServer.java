import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Message server that asynchronously handles various connections
 */
public class MessageServer {

  public static void main(String[] args) {
    if (args.length == 1){
      MessageServerAPI messageServerAPI = new MessageServerAPI();

      //repeatedly wait for connections
      while (true){
        try (ServerSocket serverSocket = new ServerSocket(Integer.valueOf(args[0]));) {

          Socket socket = serverSocket.accept();
          messageServerAPI.parseAPIRequest(socket);

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }else {
      System.out.println("Please provide the port to run the Server on");
    }
  }
}
