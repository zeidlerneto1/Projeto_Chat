package projeto_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GerenciadorDeClientes extends Thread {

    private Socket cliente;
    private String nomeCliente;
    private BufferedReader leitor;
    private PrintWriter escritor;
    private static final Map<String, GerenciadorDeClientes> clientes = new HashMap<>();

    public GerenciadorDeClientes(Socket cliente) {
        this.cliente = cliente;
        start();
    }

    @Override
    public void run() {
        try {
            leitor = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            escritor = new PrintWriter(cliente.getOutputStream(), true);

            efetuarLogin();

            String msg;
            while (true) {
                msg = leitor.readLine();
                if (msg == null || msg.isEmpty()) {
                    // O cliente fechou a conexão
                    break;
                }

                if (msg.equals(Comandos.LISTA_USUARIOS)) {
                    atualizarListaUsuarios();
                } else if (msg.equals(Comandos.SAIR)) {
                    clientes.remove(this.nomeCliente);
                    atualizarListaUsuarios();
                    break;
                } else if (msg.startsWith(Comandos.MENSAGEM)) {
                    // Mensagem privada, trate de acordo
                    processarMensagemPrivada(msg);
                } else if (msg.startsWith(Comandos.HELP)){
                  enviarMensagemHelp();
                }
                else {
                    enviarMensagemParaTodos(this.nomeCliente + ": " + msg);
                }
            }

        } catch (IOException e) {
            System.err.println("O cliente fechou a conexão");
            clientes.remove(this.nomeCliente);
            atualizarListaUsuarios();
            e.printStackTrace();
        }
    }

    private void efetuarLogin() throws IOException {
        while (true) {
            escritor.println(Comandos.LOGIN);
            this.nomeCliente = leitor.readLine().toLowerCase().replaceAll(",", "");
            if (this.nomeCliente.equalsIgnoreCase("null") || this.nomeCliente.isEmpty()) {
                escritor.println(Comandos.LOGIN_NEGADO);
            } else if (clientes.containsKey(this.nomeCliente)) {
                escritor.println(Comandos.LOGIN_NEGADO);
            } else {
                escritor.println(Comandos.LOGIN_ACEITO);
                escritor.println("Olá " + this.nomeCliente);
                clientes.put(this.nomeCliente, this);
                atualizarListaUsuarios();
                break;
            }
        }
    }

    private void atualizarListaUsuarios() {
        StringBuilder str = new StringBuilder();
        for (String c : clientes.keySet()) {
            if (this.nomeCliente.equals(c)) {
                continue;
            }
            str.append(c);
            str.append(",");
        }
        if (str.length() > 0) {
            str.delete(str.length() - 1, str.length());
        }
        for (GerenciadorDeClientes cliente : clientes.values()) {
            cliente.getEscritor().println(Comandos.LISTA_USUARIOS);
            cliente.getEscritor().println(str.toString());
        }
    }

    private void enviarMensagemParaTodos(String mensagem) {
        for (GerenciadorDeClientes cliente : clientes.values()) {
            cliente.getEscritor().println(mensagem);
        }
    }

    private void processarMensagemPrivada(String mensagem) {
        // Extrai o nome do destinatário da mensagem
        String[] partes = mensagem.split(" ", 3);
        if (partes.length == 3) {
            String destinatario = partes[1];
            String conteudo = partes[2];

            // Envia a mensagem privada apenas para o destinatário, se existir
            GerenciadorDeClientes destinatarioCliente = clientes.get(destinatario);
            if (destinatarioCliente != null) {
                destinatarioCliente.getEscritor().println(this.nomeCliente + " (privado): " + conteudo);
            } else {
                // Informa ao remetente que o destinatário não existe
                escritor.println("Usuário '" + destinatario + "' não encontrado.");
            }
        }
    }
    private void enviarMensagemHelp() {
    // Lista de comandos disponíveis
    String helpMessage = "Comandos disponíveis:\n" +
            Comandos.LISTA_USUARIOS + " - Listar usuários conectados\n" +
            Comandos.SAIR + " - Sair do chat\n" +
            Comandos.MENSAGEM + " usuário mensagem - Enviar mensagem privada\n" +
            Comandos.LOGIN + " - Efetuar login\n" +
            Comandos.HELP + " - Mostrar esta mensagem de ajuda";
    escritor.println(helpMessage);
}

    public PrintWriter getEscritor() {
        return escritor;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }
}
