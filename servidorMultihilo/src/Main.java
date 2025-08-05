import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public void init() throws IOException {
        ServerSocket server = new ServerSocket(8050);
        boolean isAlive = true;
        while (isAlive) {
            System.out.println("Esperando cliente...");
            Socket socket = server.accept();
            System.out.println("¡Cliente conectado!");
            dispatchWorker(socket);
        }
    }

    public void dispatchWorker(Socket socket) {
        new Thread(() -> {
            try {
                handleRequest(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void handleRequest(Socket socket) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("GET")) {
                var resource = line.split(" ")[1].replace("/", "");
                if (resource.isEmpty()) resource = "index.html";
                System.out.println("El cliente está pidiendo: " + resource);
                sendResponse(socket, resource);
            }
        }
    }

    public void sendResponse(Socket socket, String resource) throws IOException {
        File file = new File("resources/" + resource);

        if (file.exists()) {
            String extension = getFileExtension(resource);
            String mimeType = getMimeType(extension);

            var writer = new BufferedOutputStream(socket.getOutputStream());

            byte[] content = readFileContent(file);

            writer.write(("HTTP/1.1 200 OK\r\n").getBytes());
            writer.write(("Content-Type: " + mimeType + "\r\n").getBytes());
            writer.write(("Content-Length: " + content.length + "\r\n").getBytes());
            writer.write("Connection: close\r\n\r\n".getBytes());

            writer.write(content);
            writer.flush();
            writer.close();
            socket.close();

        } else {
            // 404 Not Found
            var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String notFound = "<html><body><h1>404 - Archivo no encontrado</h1></body></html>";

            writer.write("HTTP/1.1 404 Not Found\r\n");
            writer.write("Content-Type: text/html\r\n");
            writer.write("Content-Length: " + notFound.length() + "\r\n");
            writer.write("Connection: close\r\n\r\n");
            writer.write(notFound);
            writer.flush();
            writer.close();
            socket.close();

            System.out.println("No se encontró el archivo: " + resource);
        }
    }

    private String getMimeType(String extension) {
        if (extension.equals("html") || extension.equals("htm")) {
            return "text/html";
        } else if (extension.equals("jpg") || extension.equals("jpeg")) {
            return "image/jpeg";
        } else if (extension.equals("gif")) {
            return "image/gif";
        } else if (extension.equals("png")) {
            return "image/png";
        } else if (extension.equals("txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream"; // valor por defecto
        }
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index == -1 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    private byte[] readFileContent(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        fis.close();
        return baos.toByteArray();
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.init();
    }
}
