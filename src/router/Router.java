package router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {

    public static void main(String[] args) throws IOException {
        Semaphore mutex = new Semaphore(0);

        // Lista de endereços IP dos vizinhos
        ArrayList<String> ip_list = new ArrayList<String>();

        // Lê arquivo de entrada com lista de IP dos roteadores vizinhos
        try (BufferedReader inputFile = new BufferedReader(new FileReader("neighbors-ips.txt"))) {
            String ip;

            while ((ip = inputFile.readLine()) != null) {
                ip_list.add(ip);
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, e);
            return;
        }

        // Cria instâncias da tabela de roteamento e das threads de envio e recebimento de mensagens
        RouterTable router_table = new RouterTable();
        Thread sender = new Thread(new MessageSender(router_table, ip_list, mutex));
        Thread receiver = new Thread(new MessageReceiver(router_table));

        sender.start();
        receiver.start();
    }
}