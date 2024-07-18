package projeto_chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Interface_Cliente extends javax.swing.JFrame {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private List<String> connectedUsers;
    private DefaultListModel<String> usersListModel;

    public Interface_Cliente() {
        initComponents();
    }

    private void initComponents() {
        chatArea = new javax.swing.JTextArea();
        messageInput = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        usersList = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        chatArea.setEditable(false);
        chatArea.setColumns(20);
        chatArea.setRows(5);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        sendButton.setText("Enviar");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        usersList.setModel(new DefaultListModel<>());
        usersListModel = (DefaultListModel<String>) usersList.getModel();

        // Barra de ferramentas
        JToolBar toolbar = new JToolBar();
        JButton connectButton = new JButton("Connect");
        JButton disconnectButton = new JButton("Disconnect");

        // Adiciona ações aos botões
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnectFromServer();
            }
        });

        // Adiciona botões à barra de ferramentas
        toolbar.add(connectButton);
        toolbar.add(disconnectButton);

        // Adiciona a barra de ferramentas ao layout
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(scrollPane)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(messageInput, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sendButton)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(usersList, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(toolbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(usersList, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addComponent(scrollPane))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(messageInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sendButton))
                    .addContainerGap())
        );

        pack();
    }

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String message = messageInput.getText();

        if (!message.isEmpty()) {
            if (message.startsWith(Comandos.MENSAGEM)) {
                // Este é um comando de mensagem privada, trate de acordo
                String[] parts = message.split(" ", 3);
                if (parts.length == 3) {
                    // Envia a mensagem privada no formato: /msg destinatario mensagem
                    writer.println(message);
                } else {
                    // Exiba uma mensagem de erro, formato incorreto
                    System.out.println("Formato incorreto para mensagem privada.");
                }
            } else if (message.equals(Comandos.LISTA_USUARIOS)) {
                // O usuário quer obter a lista de usuários conectados
                writer.println(Comandos.LISTA_USUARIOS);
            } else if (message.equals(Comandos.SAIR)) {
                // O usuário quer sair, envie o comando de sair
                writer.println(Comandos.SAIR);
            } else {
                // Esta é uma mensagem regular, envie ao servidor
                writer.println(message);
            }

            messageInput.setText("");
        }
    }

    private void connectToServer() {
        try {
            String serverIP = JOptionPane.showInputDialog(this, "Enter Server IP:", "Server IP", JOptionPane.QUESTION_MESSAGE);
            String serverPort = JOptionPane.showInputDialog(this, "Enter Server Port:", "Server Port", JOptionPane.QUESTION_MESSAGE);

            if (serverIP == null || serverPort == null || serverIP.isEmpty() || serverPort.isEmpty()) {
                // O usuário cancelou ou não forneceu informações
                return;
            }

            int port = Integer.parseInt(serverPort);
            socket = new Socket(serverIP, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connectedUsers = new ArrayList<>();

            new Thread(this::handleServerMessages).start();

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        // Adicione código para desconectar do servidor
        try {
            if (socket != null && socket.isConnected()) {
                writer.println(Comandos.SAIR); // Informa ao servidor que está saindo
                socket.close();
                reader.close();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("USERS:")) {
                    updateUsersList(message.substring(6));
                } else {
                    chatArea.append(message + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUsersList(String users) {
        usersList.setModel(new DefaultListModel<>());  // Limpando a lista
        String[] usersArray = users.split(",");
        connectedUsers.clear();

        for (String user : usersArray) {
            connectedUsers.add(user);
            ((DefaultListModel<String>) usersList.getModel()).addElement(user);
        }
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Interface_Cliente cliente = new Interface_Cliente();
                cliente.setVisible(true);
            }
        });
    }

    private javax.swing.JTextArea chatArea;
    private javax.swing.JTextField messageInput;
    private javax.swing.JButton sendButton;
    private javax.swing.JList<String> usersList;
}
