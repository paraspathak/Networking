package app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * ServerHelper Called for each and every request by the server
 */
public class ServerHelper extends Thread {
    private Socket socket = null;
    private final File ROOT = new File(".");                                                            // Root directory where html file and image should be
    private final String DEFAULTFILE = "index.html";                                                    //change if you want a new index file

    public ServerHelper(Socket socket) {    //Constructor
        this.socket = socket;   //Store the socket
        System.out.println("New Client found ....");
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));     //Create a buffered reader to get the HTTP request code
            BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream());          //Buffered output stream to write the outgoinh hTTP message
            PrintWriter out = new PrintWriter(socket.getOutputStream());                                //Second stream to handle outgoing message
            String msg = in.readLine();                                                                 //read the first line of the request
            System.out.println("First line of Message is " + msg);                                      //echo the request

            StringTokenizer parse = new StringTokenizer(msg);                                           // tokenize the message to the get the contents of it
            String method = parse.nextToken().toUpperCase();                                            // Get to see which HTTP request is made
            String file_requested = parse.nextToken();                                                  // Get which file is requested

            if (method.equals("GET")) {                                                                 // Get method
                System.out.println("file requested is: " + file_requested);
                if (file_requested.equals("/")) {                                                       // Return home page
                    String file_to_send = file_requested + DEFAULTFILE;                                 //create the file directory for the default home file
                    File file = new File(ROOT, file_to_send);                                           // Create a file for index.html
                    int file_length = (int) file.length();                                              // Get the length for HTML message
                    System.out.println("file length is: " + file_length);

                    String content = getContentType(file_to_send);                                      // Get the MIME Type of the HTML

                    byte[] file_data = readFileData(file, file_length);                                 // Get the file content as byte[]

                    //Format of the HTTP message
                    out.println("HTTP/1.1 200 OK");                                                     // Success code
                    out.println("Server: Java HTTP Server from Paras Pathak : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);                                            // Add the content type of the return statement
                    out.println("Content-length: " + file_length);
                    out.println();                                                                      // blank line between headers and content
                    out.flush();                                                                        // flush the stream buffer

                    dataOut.write(file_data, 0, file_length);                                           // Write the content of the file after the headers
                    dataOut.flush();                                                                    // flush the stream buffer

                    // Update the console
                    System.out.println("File " + file_requested + " of type " + content + " returned");
                    out.close();                                                                        //close both streams 
                    dataOut.close();
                } else if (file_requested.matches("^\\/[A-Za-z]+$")) {                                  //request for anything other than / ie /images -> 
                    try {                                                                               //should send a http 404 not found
                        //format the message
                        out.println("HTTP/1.1 404 Not Found");                                          // Failure code
                        out.println("Server: Java HTTP Server from Paras Pathak : 1.0");
                        out.println("Date: " + new Date());
                        out.println();                                                                  // blank line between headers and content
                        out.flush();                                                                    // flush the stream buffer
                        out.close();
                        dataOut.close();

                    } catch (Exception e) {
                        System.out.println("file" + file_requested + " not found!!" + e.toString());
                        // Send not found
                        out.println("HTTP/1.1 404 Not Found");
                    }

                } else {                                                                                // See if there's file or not in the current directory
                    try {
                        String file_to_send = file_requested;
                        File file = new File(ROOT, file_to_send);                                       // Create a file for asked parameters
                        if (file.exists()) {                                                            //if there is file
                            String content = getContentType(file_to_send);                              // Get the MIME Type of the HTML
                            int file_length = (int) file.length();                                      // Get the length for HTML message

                            out.println("HTTP/1.1 200 OK");                                             // Success code
                            out.println("Server: Java HTTP Server from Paras Pathak : 1.0");
                            out.println("Date: " + new Date());
                            out.println("Content-type: " + content);                                    // Add the content type of the return statement
                            out.println("Content-length: " + file_length);
                            out.println();                                                              // blank line between headers and content
                            out.flush();                                                                // flush the stream buffer

                            OutputStream outputStream = socket.getOutputStream();                       //instatiate a new stream to write out
                            Files.copy(file.toPath(), outputStream);                                    //copy the contents of the file
                            out.close();                                                                //close the streams
                            dataOut.close();
                            outputStream.close();

                        } else {                                                                        //moved block -> called when there's no file in the directory
                            out.println("HTTP/1.1 301 Moved Permanently");                              // Moved code
                            out.println("Location: http://localhost:8090");                             //Send the homepage link 
                            out.println("Server: Java HTTP Server from Paras Pathak : 1.0");
                            out.println("Date: " + new Date());                                         //Date of message
                            out.println();                                                              // blank line between headers and content
                            out.flush();                                                                // flush the stream buffer
                            //close streams
                            out.close();
                            dataOut.close();
                        }

                    } catch (Exception e) {
                        System.out.println("file" + file_requested + " not found!!" + e.getMessage());
                        // Send not found
                        out.println("HTTP/1.1 404 Not Found");
                    }
                }
            } else {                                                                                    // if (method.equals("POST")){ Implement other methods here
                System.out.println("501 Not Implemented :" + method + "method.");
                out.println("HTTP/1.1 404 Not Found");
            }
        } catch (IOException e) {
            System.out.println("Cannot get input stream of client");
            System.out.println(e.getMessage());
        }
    }

    //reads the files in specified path and returns the byte[] of it
    private byte[] readFileData(File file, int fileLength) throws IOException { 
        FileInputStream fileIn = null;
        byte[] file_to_return = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(file_to_return);
        } finally {
            if (fileIn != null) {
                fileIn.close();
            }
        }
        return file_to_return;
    }

    // return supported MIME Types
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else
            return "text/plain";
    }
}