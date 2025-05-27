import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final List<String> chatLog = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Digite a porta para o servidor: ");
        int port = Integer.parseInt(scanner.nextLine());

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado na porta " + port + ". Aguardando clientes...");

        new Thread(() -> {
            Scanner serverAdminScanner = new Scanner(System.in);
            while (true) {
                System.out.println("Digite 'exportar' para salvar o chat ou 'sair' para fechar o servidor:");
                String command = serverAdminScanner.nextLine();
                if ("exportar".equalsIgnoreCase(command)) {
                    exportChatLog();
                } else if ("sair".equalsIgnoreCase(command)) {
                    System.out.println("Encerrando o servidor...");
                    try {
                        
                        for (ClientHandler client : clients) {
                            client.sendMessage("Servidor encerrando.");
                            client.closeConnection();
                        }
                        serverSocket.close();
                        pool.shutdown();
                    } catch (IOException e) {
                        System.err.println("Erro ao fechar o servidor: " + e.getMessage());
                    }
                    System.exit(0);
                    break;
                }
            }
            serverAdminScanner.close();
        }).start();


        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Cliente conectado: " + clientSocket.getRemoteSocketAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket);
                clients.add(clientThread);
                pool.execute(clientThread);
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    System.out.println("Servidor socket foi fechado.");
                    break;
                }
                System.err.println("Erro ao aceitar conexão do cliente: " + e.getMessage());
            }
        }
    }

    static synchronized void broadcastMessage(String message, ClientHandler sender) {
        chatLog.add(message);
        System.out.println(message); 
        for (ClientHandler client : clients) {
            if (client != sender) { 
                 client.sendMessage(message);
            }
        }
    }

    static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Cliente desconectado: " + client.getClientAddress());
    }

    static synchronized void exportChatLog() {
        try (PrintWriter out = new PrintWriter(new FileWriter("chat_log_server.txt", true))) { // true para append
            out.println("\n--- Log exportado em: " + new java.util.Date() + " ---");
            for (String message : chatLog) {
                out.println(message);
            }
            System.out.println("Log do chat exportado para chat_log_server.txt");
        } catch (IOException e) {
            System.err.println("Erro ao exportar log do chat: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientAddress;
    private String clientName; // Nome do cliente

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.clientAddress = socket.getRemoteSocketAddress().toString();
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String getClientAddress() {
        return clientAddress;
    }

    @Override
    public void run() {
        try {
            // Solicitar nome ao cliente
            out.println("Bem-vindo ao Chat! Por favor, digite seu nome:");
            clientName = in.readLine();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Anônimo" + clientAddress;
            }
            ChatServer.broadcastMessage("Servidor: " + clientName + " entrou no chat.", this);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(inputLine.trim())) {
                    break;
                }
                ChatServer.broadcastMessage(clientName + ": " + inputLine, null); // null para broadcast para todos
            }
        } catch (SocketException e){
            System.err.println("Conexão perdida com " + (clientName != null ? clientName : clientAddress) + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro no handler do cliente " + (clientName != null ? clientName : clientAddress) + ": " + e.getMessage());
        } finally {
            closeConnection();
            ChatServer.removeClient(this);
            ChatServer.broadcastMessage("Servidor: " + (clientName != null ? clientName : "Alguém") + " saiu do chat.", null);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão com " + (clientName != null ? clientName : clientAddress) + ": " + e.getMessage());
        }
    }
}