## Explicação do Servidor:

## ChatServer classe principal:

    - **clients:** Lista para armazenar manipuladores de clientes conectados.

    - **chatLog:** Lista para armazenar todas as mensagens do chat para exportação.

    - **pool:** Um ExecutorService (pool de threads) para gerenciar as threads dos clientes de forma eficiente.

    - **main():** Pede a porta, inicia o ServerSocket. Em um loop infinito, aguarda conexões de clientes (serverSocket.accept()).
                Cada nova conexão é passada para um ClientHandler que é executado em uma nova thread do pool.

    - **Thread de Comandos do Servidor:** Uma thread separada é iniciada para permitir que o administrador do servidor digite comandos como exportar ou sair sem bloquear o
                                        loop principal de aceitação de clientes.

    - **broadcastMessage():** Envia uma mensagem para todos os clientes conectados e a adiciona ao chatLog.

    - **removeClient():** Remove um cliente da lista quando ele se desconecta.
    
    - **exportChatLog():** Salva o conteúdo de chatLog em um arquivo chat_log_server.txt.

    - **ClientHandler classe (Runnable):** Cada instância desta classe lida com um único cliente.

    - **run():**  Solicita um nome ao cliente.
                Lê mensagens do cliente (in.readLine()).
                Se a mensagem for "exit", encerra o loop e a conexão.
                Caso contrário, chama ChatServer.broadcastMessage() para enviar a mensagem aos outros clientes.

    - **sendMessage():** Envia uma mensagem para este cliente específico.

    - **closeConnection():** Fecha os streams e o socket do cliente.
