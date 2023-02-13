import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            var engine = new BooleanSearchEngine(new File("pdfs"));
            var server = new Server(8085, engine);
            server.start();
        } catch (IOException e) {
            System.out.println("Ошибка создания движка поиска.");
            e.printStackTrace();
        }

    }
}