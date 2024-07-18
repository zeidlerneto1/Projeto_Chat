/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package projeto_chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerInterface extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, GerenciadorDeClientes> clientesConectados = new HashMap<>();
    private DefaultListModel<String> userListModel;

    public ServerInterface() {
        super("Server Interface");

        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);

        JButton startButton = new JButton("Iniciar Server");
        JButton stopButton = new JButton("Parar Server");

        JPanel controlPanel = new JPanel();
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = JOptionPane.showInputDialog("Insira IP:");
                String portStr = JOptionPane.showInputDialog("Insira Porta:");
                int port = Integer.parseInt(portStr);
                startServer(ip, port);
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        setLayout(new BorderLayout());
        add(userScrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServer(String ip, int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server iniciou no " + ip + ":" + port);

            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        GerenciadorDeClientes gerenciador = new GerenciadorDeClientes(clientSocket);
                        clientesConectados.put(gerenciador.getNomeCliente(), gerenciador);
                        atualizarListaUsuarios();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server parado");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

public void removerCliente(GerenciadorDeClientes gerenciador) {
    SwingUtilities.invokeLater(() -> {
        clientesConectados.remove(gerenciador.getNomeCliente());
        atualizarListaUsuarios();
    });
}


private void atualizarListaUsuarios() {
    SwingUtilities.invokeLater(() -> {
        userListModel.clear();
        for (String username : clientesConectados.keySet()) {
            userListModel.addElement(username);
        }
    });
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerInterface());
    }
}


