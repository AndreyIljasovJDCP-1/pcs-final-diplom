import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try (var serverSocket = new ServerSocket(8989)) {
            var engine = new BooleanSearchEngine(new File("pdfs"));
            Gson gson = new Gson();

            System.out.println("Server started....");

            while (true) {
                try (var clientSocket = serverSocket.accept();
                     var out = new PrintWriter(clientSocket.getOutputStream(), true);
                     var in = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()))) {

                    var words = in.readLine();
                    var result = engine.search(words);
                    var response = gson.toJson(result);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}