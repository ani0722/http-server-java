import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            // Continuously accept incoming connections and spawn a new thread for each client.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");
                new Thread(() -> handleClient(clientSocket)).start();  // Start a new thread to handle the client.
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            // Read the first line of the request.
            String line = reader.readLine();
            String[] HttpRequest = line.split(" ");
            String[] str = HttpRequest[1].split("/");

            // Check the path and respond accordingly.
            if (HttpRequest[1].equals("/")) {
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n";
                output.write(response.getBytes());
            } else if (str[1].equals("user-agent")) {
                reader.readLine();
                String useragent = reader.readLine().split("\\s+")[1];
                String reply = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n%s\r\n",
                        useragent.length(), useragent);
                output.write(reply.getBytes());
            } else if ((str.length > 2 && str[1].equals("echo"))) {
                String responsebody = str[2];
                String finalstr = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + responsebody.length() +
                        "\r\n\r\n" + responsebody;
                output.write(finalstr.getBytes());
            } else {
                output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }

            output.flush();
            clientSocket.close();  // Close the connection after sending the response.
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }
}
