package router;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class RouterTable {
    private HashMap<String, Route> routerTable;
    private ArrayList<String> neighbors;
    private String localhostIP;

    public RouterTable(ArrayList<String> neighbors, String localhost_ip) {
        this.routerTable = new HashMap<String, Route>();
        this.neighbors = neighbors;
        this.localhostIP = localhost_ip;
    }

    public boolean updateTable(String table_string, InetAddress sender_address) {
        boolean table_updated = false;
        table_string = table_string.trim();
        String sender_ip = sender_address.getHostAddress();

        // Verifica se a tabela recebida está vazia
        if (table_string.equals("!")) {
            if (!this.routerTable.containsKey(sender_ip)) {
                table_updated = true;
            }
            this.routerTable.put(sender_ip, new Route(sender_ip, 1, sender_ip));
        } else {
            String[] table_rows = table_string.substring(1).split("\\*");

            // Percorre as linhas da tabela recebida
            for (int i = 0; i < table_rows.length; i++) {
                String[] table_row = table_rows[i].split(";");

                String destination_ip = table_row[0].trim();
                int metric = Integer.parseInt(table_row[1]);

                // Verifica se o próprio IP foi enviado por um outro roteador
                if (destination_ip.equals(this.localhostIP)) {
                    // Verifica se quem enviou é um roteador vizinho
                    if (metric == 1) {
                        if (!this.routerTable.containsKey(sender_ip)) {
                            table_updated = true;
                        }
                        this.routerTable.put(sender_ip, new Route(sender_ip, 1, sender_ip));
                    }
                } else if (this.routerTable.containsKey(destination_ip)) {
                    /*
                     * Se o IP de destino da tabela recebida já existe na tabela
                     * de roteamento local, verifica se a metrica é menor que a
                     * contida na tabela de roteamento local antes de atualizar a
                     * rota
                     */
                    if (metric + 1 < this.routerTable.get(destination_ip).getMetric()) {
                        this.routerTable.put(destination_ip, new Route(destination_ip, metric + 1, sender_ip));
                        table_updated = true;
                    } else {
                        if (this.neighbors.contains(destination_ip)) {
                            this.routerTable.get(destination_ip).setReceivedDate(new Date());
                        }
                    }
                } else {
                    if (!this.neighbors.contains(destination_ip)) {
                        // Insere a nova rota na tabela de roteamento local
                        this.routerTable.put(destination_ip, new Route(destination_ip, metric + 1, sender_ip));
                        table_updated = true;
                    }
                }
            }
        }

        return table_updated;
    }

    public String getTableString() {
        String table_string = "";

        // Verifica se a tabela de rotemento local está vazia
        if (!this.routerTable.isEmpty()) {
            // Transforma a tabela de roteamento local no formato em string da especificação
            for (HashMap.Entry<String, Route> entry : this.routerTable.entrySet()) {
                Route route = entry.getValue();

                table_string += "*";
                table_string += route.getDestinationIP();
                table_string += ";";
                table_string += route.getMetric();
            }
        } else {
            // Tabela de roteamento vazia
            table_string = "!";
        }

        return table_string;
    }

    public boolean removeInactiveRouters() {
        ArrayList<String> garbage = new ArrayList<String>();

        Date current_date = new Date();

        for (HashMap.Entry<String, Route> entry : this.routerTable.entrySet()) {
            Route route = entry.getValue();
            String destination_ip = route.getDestinationIP();

            if (current_date.getTime() - route.getReceivedDate().getTime() > 30000) {
                garbage.add(destination_ip);
            }
        }

        for (String destination_ip : garbage) {
            this.routerTable.remove(destination_ip);
        }

        return !garbage.isEmpty();
    }

    public String toString() {
        Route route;
        SimpleDateFormat date_format = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");

        String header = "*** Tabela de roteamento local ***\n";

        String body = "";
        for (HashMap.Entry<String, Route> entry : this.routerTable.entrySet()) {
            route = entry.getValue();
            body += "IP de destino: " + route.getDestinationIP() + "\n";
            body += "Métrica: " + route.getMetric() + "\n";
            body += "IP de saída: " + route.getOutgoingIP() + "\n";
            body += "Data de recebimento: " + date_format.format(route.getReceivedDate()) + "\n\n";
        }

        if (body.isEmpty()) {
            body = "[vazia]\n";
        }

        return header + body;
    }
}