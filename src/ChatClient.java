import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatClient {
    private static List<String> clientChatLog = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite o endereço IP do servidor: ");
        String serverAddress = scanner.nextLine();
        System.out.print("Digite a porta do servidor: ");
        int serverPort = Integer.parseInt(scanner.nextLine());

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner consoleInput = new Scanner(System.in)) {

            System.out.println("Conectado ao servidor: " + serverAddress + ":" + serverPort);

            // Thread para receber mensagens do servidor
            Thread serverListener = new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                        clientChatLog.add(serverMessage); // Adiciona ao log do cliente
                        if ("Servidor encerrando.".equals(serverMessage)) {
                            System.out.println("Desconectando...");
                            break;
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Conexão com o servidor perdida.");
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                       System.err.println("Erro ao ler do servidor: " + e.getMessage());
                    }
                } finally {
                     System.out.println("Desconectado do servidor.");
                     // Tenta fechar o socket do lado do cliente se ainda estiver aberto
                    try {
                        if (!socket.isClosed()) {
                            socket.close();
                        }
                    } catch (IOException ex) {
                        // Ignorar
                    }
                    System.exit(0); // Encerra o cliente se o servidor cair ou fechar a conexão
                }
            });
            serverListener.start();

            // Loop principal para enviar mensagens
            String userInput;
            System.out.println("Você pode digitar 'exit' para sair ou 'exportar' para salvar seu chat.");
            while (true) {
                userInput = consoleInput.nextLine();
                clientChatLog.add("Você: " + userInput); // Adiciona ao log do cliente

                if ("exportar".equalsIgnoreCase(userInput)) {
                    exportClientChatLog();
                    continue; // Não envia 'exportar' para o servidor
                }
                
                out.println(userInput); // Envia a mensagem para o servidor
                if ("exit".equalsIgnoreCase(userInput.trim())) {
                    System.out.println("Encerrando conexão...");
                    break;
                }
            }
            serverListener.join(); // Espera a thread de escuta terminar

        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + serverAddress);
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor: " + serverAddress + ":" + serverPort + " - " + e.getMessage());
        } catch (InterruptedException ex) {
            System.getLogger(ChatClient.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } finally {
            scanner.close();
            System.out.println("Cliente encerrado.");
        }
    }

    private static synchronized void exportClientChatLog() {
        try (PrintWriter fileOut = new PrintWriter(new FileWriter("chat_log_client.txt", true))) { // true para append
            fileOut.println("\n--- Log exportado em: " + new java.util.Date() + " ---");
            for (String message : clientChatLog) {
                fileOut.println(message);
            }
            System.out.println("Log do chat do cliente exportado para chat_log_client.txt");
        } catch (IOException e) {
            System.err.println("Erro ao exportar log do chat do cliente: " + e.getMessage());
        }
    }
}