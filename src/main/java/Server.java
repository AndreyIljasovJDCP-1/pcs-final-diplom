import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;

public class Server {

    private final int port;
    private final BooleanSearchEngine engine;

    public Server(int port, BooleanSearchEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() {
        try (var serverSocket = new ServerSocket(port)) {
            Gson gson = new Gson();
            System.out.println("Server started....");

            while (true) {
                try (var clientSocket = serverSocket.accept();
                     var out = new PrintWriter(clientSocket.getOutputStream(), true);
                     var in = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()))) {

                    var request = in.readLine();
                    var result = engine.search(request);
                    var response = gson.toJson(result);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
