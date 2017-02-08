import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Message server that asynchronously handles various connections
 */
public class MessageServer {

  public static void main(String[] args) {
    MessageServerAPI messageServerAPI = new MessageServerAPI();

    //repeatedly wait for connections
    while (true){
      try (ServerSocket serverSocket = new ServerSocket(8080);) {

        Socket socket = serverSocket.accept();
        messageServerAPI.parseAPIRequest(socket);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
