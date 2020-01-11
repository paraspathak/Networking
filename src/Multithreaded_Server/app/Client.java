package app;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * client Not needed. Usedd to test, already removed major parts
 */
public class Client extends Thread {
    private String message;
    private Socket socket;

    public Client(String msg, int port, InetAddress server_address) {
        System.out.println("Client running ");
        this.message = msg;
        try {
            this.socket = new Socket(server_address, port);
        } catch (Exception e) {
            System.out.println("Error in creating a socket");
        }
    }

    public void run() {
        try {
            OutputStream out = this.socket.getOutputStream();
            System.out.println("message is: "+this.message);
            out.write(this.message.getBytes());
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Client client = new Client("hello world", 8090, InetAddress.getLocalHost());
        client.run();
    }

}