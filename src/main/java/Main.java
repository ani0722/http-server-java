import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static String directory;

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 1 && args[0].equals("--directory")) {
            directory = args[1];
        }

        System.out.println("Logs from your program will appear here!");

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream output = clientSocket.getOutputStream()) {

            String line = reader.readLine();
            if (line == null) return;

            String[] request = line.split(" ");
            String urlPath = request[1];
            Map<String, String> headers = new HashMap<>();
            String response = getHttpResponse(urlPath, headers);

            output.write(response.getBytes());
            output.flush();
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static String getHttpResponse(String urlPath, Map<String, String> headers) throws IOException {
        String httpResponse;

        if ("/".equals(urlPath)) {
            httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);

            if (file.exists()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                headers.put("Content-Type", "application/octet-stream");
                headers.put("Content-Length", String.valueOf(fileContent.length));

                // Build the HTTP response headers
                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("HTTP/1.1 200 OK\r\n");
                headers.forEach((key, value) -> headerBuilder.append(key).append(": ").append(value).append("\r\n"));
                headerBuilder.append("\r\n");

                httpResponse = headerBuilder.toString() + new String(fileContent);
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        return httpResponse;
    }
}
