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

            // Parse headers
            String headerLine;
            while (!(headerLine = reader.readLine()).isEmpty()) {
                String[] headerParts = headerLine.split(": ");
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

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
        } else if ("/user-agent".equals(urlPath)) {  // Add this condition for /user-agent
            String userAgent = headers.getOrDefault("User-Agent", "unknown");
            httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + userAgent.length() + "\r\n\r\n" + userAgent;
        } else if (urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);
            if (file.exists()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                        fileContent.length + "\r\n\r\n";
                httpResponse += new String(fileContent);
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        return httpResponse;
    }
}
