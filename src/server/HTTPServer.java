package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
    private static int port = 8080;
    private static String hostname = "127.0.0.1";
    private Socket clientSocket;
    private volatile int threadCount = 0;
    Object obj = new Object();

    public class HTTPThread extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private OutputStream out;

        HTTPThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            clientSocket.setSoTimeout(1000);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = clientSocket.getOutputStream();
        }

        public void listen() throws IOException {
            String request_frame = "", line = "";
            while((line = in.readLine()) != null) {
                request_frame = request_frame + line + "\r\n";
                if (!in.ready() && request_frame.length() > 4) {
                    byte[] outputLine;
                    HTTPProtocol proto = new HTTPProtocol(request_frame);
                    outputLine = proto.processReq();
                    out.write(outputLine);
                    request_frame = "";
                }
            }
        }

        public void run() {
            try {
                listen();
                clientSocket.close();
                synchronized (obj) {
                    System.out.println("Thread Closed - " + threadCount);
                    threadCount--;
                }
            }  catch (Exception e) {
                try {
                    clientSocket.close();
                } catch (IOException f) {
                    f.printStackTrace();
                }
                synchronized (obj) {
                    System.out.println("Thread Closed - " + threadCount);
                    threadCount--;
                }
                e.printStackTrace();
            }
        }
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server Started");
            while (true) {
                if(threadCount < 100) {
                    clientSocket = serverSocket.accept();
                    HTTPThread clientThread = new HTTPThread(clientSocket);
                    synchronized (obj) {
                        threadCount++;
                    }
                    clientThread.start();
                    System.out.println("Thread Created - " + threadCount);
                } else Thread.currentThread().sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        HTTPServer server = new HTTPServer();
        server.startServer();
    }
}
