package app;

import java.net.ServerSocket;
import java.io.IOException;
import app.ServerHelper;

/**
 * Server Class
 */
public class Server {
    //Constructs the main server, input none output instance of Server class
    public Server() throws IOException {                                        //Throws exception if it can not open socket
        System.out.println("Server starting ... ");             
        final ServerSocket server = new ServerSocket(8090);                     //Create a new server and put the listening host as 8090
        System.out.println("Listening for clients at localhost:8090 ...");
        boolean listen_for_clients = true;
        while (listen_for_clients) {
            ServerHelper serverHelper = new ServerHelper(server.accept());      //Listen for new clients here and
            serverHelper.run();                                                 //Run a new thread for any new connection 
        }
        server.close(); //Close the opened server
    }

}