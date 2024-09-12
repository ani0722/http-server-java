import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static String filesDirectory = "/tmp/"; // Default directory

    public static void main(String[] args) {
        if (args.length > 1 && args[0].equals("--directory")) {
            filesDirectory = args[1];
        }

        System.out.println("Logs from your program will appear here!");

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line = reader.readLine();
            if (line == null) return;

            String[] HttpRequest = line.split(" ");
            String path = HttpRequest[1];

            if (path.startsWith("/files/")) {
                String filename = path.substring(7); // Extract the filename
                File file = new File(filesDirectory + filename);

                if (file.exists() && !file.isDirectory()) {
                    byte[] fileContent = readFileToByteArray(file);
                    String responseHeader = String.format(
                            "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n",
                            fileContent.length);
                    output.write(responseHeader.getBytes());
                    output.write(fileContent); // Write the file content as the response body
                } else {
                    String notFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                    output.write(notFoundResponse.getBytes());
                }
            } else {
                String notFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                output.write(notFoundResponse.getBytes());
            }

            output.flush();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static byte[] readFileToByteArray(File file) throws IOException {
        FileInputStream fis = null;
        byte[] fileData = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(fileData);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return fileData;
    }
}
