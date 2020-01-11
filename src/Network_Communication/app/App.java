// Network Layer simulation using java

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Base64.Decoder;
import java.util.concurrent.TimeUnit;

public class App {
    private int[][] distance_vector = new int[10][3]; // Destination Cost Next Hop
    private int NODE_ID;
    private int _destination;
    private String _msg;
    private LinkedHashMap<String, Integer> msg_translate = new LinkedHashMap<String, Integer>();
    private LinkedHashMap<String, Integer> XOR_msg_translate = new LinkedHashMap<String, Integer>();
    private int transport_send_string_loop = 0;
    private boolean transport_send_XOR = false;
    private int transport_XOR_sequence = 0;
    private String[] transport_XOR_message;
    private long[] position = {0,0,0,0,0,0,0,0,0,0};

    /*
     *****************************************
     * DATA LINK LAYER *
     *****************************************
     */

    // To send message to other node
    public void datalink_receive_from_network(String msg, int len, int next_Hop) { // encode the message into a datalink
        StringBuilder builder = new StringBuilder();
        // Create the datalink message packet
        {
            builder.append('S');
            builder.append((len + 5) < 10 ? '0' + String.valueOf(len + 5) : len + 5);
            builder.append(msg);
            // Calculate the checksum
            int checksum_value = 0;
            for (int i = 0; i < builder.length(); i++) {
                checksum_value += (int) builder.charAt(i);
            }
            builder.append(
                    (checksum_value % 100) < 10 ? '0' + String.valueOf(checksum_value % 100) : (checksum_value % 100));
        }
        String filename = "from" + this.NODE_ID + "to" + next_Hop;
        
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filename, true);
            fileOutputStream.write((builder.toString()).getBytes());
        } catch (IOException e) {
            System.err.println("IO ERROR datalink receive from node");
            System.exit(1);
        }
    }

    // To read from each of the input file
    public void datalink_receive_from_channel() {
        for (int a = 0; a < 10; a++) {
            // Only read for nodes that are reachable to this file
            if (a != NODE_ID && distance_vector[a][1] < 2) { // Here was not equals to 99
                // System.out.println("Reading file: from" + a + "to" + NODE_ID);
                String filename = "from" + a + "to" + NODE_ID;
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(filename);
                    int r;
                    boolean read_size = false;
                    int msg_size = 99;
                    int sum_ASCII = 0;
                    fileInputStream.skip(position[a]);
                    while ((r = fileInputStream.read()) != -1) {
                        char c = (char) r;
                        if (read_size) {
                            sum_ASCII += r;
                            // Get the size of the message
                            int next = fileInputStream.read();
                            if (next != -1) {
                                sum_ASCII += next; // Add the second bit for the number
                                msg_size = Integer.valueOf(String.valueOf(c) + String.valueOf((char) next));
                                StringBuilder msg_body = new StringBuilder();
                                // Now read the messge and checksum separate
                                boolean is_success = true;
                                for (int i = 0; i < msg_size - 5; i++) {
                                    int word = fileInputStream.read();

                                    if (word != -1) {
                                        sum_ASCII += word;
                                        msg_body.append((char) word);
                                    } else {
                                        read_size = false;
                                        sum_ASCII = 0;
                                        is_success = false;
                                        break;
                                    }
                                }
                                if (is_success) { // Check for the Checksum here
                                    next = fileInputStream.read();
                                    if (next != -1) {
                                        int final_w = fileInputStream.read();
                                        try {
                                            if (final_w != -1) {
                                                int checksum = Integer.valueOf(
                                                        String.valueOf((char) next) + String.valueOf((char) final_w));
                                                if (sum_ASCII % 100 == checksum) {
                                                    // Send to network layer
                                                    String network_layer_msg = msg_body.toString();
                                                    String length_packet = "" + network_layer_msg.charAt(1)+network_layer_msg.charAt(2);    // Added the next string
                                                    try {
                                                        network_receive_from_datlink(network_layer_msg,
                                                                network_layer_msg.length(),
                                                                Integer.valueOf(length_packet));

                                                    } catch (NumberFormatException e) {
                                                        System.err.println(e.getMessage());
                                                        System.err.println(
                                                                "Invalid msg found, Dumping msg" + network_layer_msg);
                                                    }
                                                } else {
                                                    System.err.println("Checksum doesnt match");
                                                    System.err.println(msg_body.toString());
                                                    System.out.printf("calc: %d actual: %d\n", sum_ASCII, checksum);
                                                }
                                            }
                                        } catch (NumberFormatException e) {
                                            System.err.println("Illegal datalink message found!" + msg_body);
                                        }
                                    }
                                }
                                read_size = false;
                                sum_ASCII = 0;
                            } else {
                                read_size = false;
                                sum_ASCII = 0;
                            }
                        }
                        if (c == 'S') { // Start of the message
                            read_size = true;
                            sum_ASCII += r;
                        }
                        position[a] = fileInputStream.getChannel().position();
                    }
                } catch (FileNotFoundException e) {
                    System.err.println("Cannot find file: " + filename + " Cannot get messages send from " + a
                            + " -------> " + NODE_ID);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("IO Exception reading file: " + filename);
                    System.exit(1);
                } catch (NumberFormatException e) {
                    System.err.println("Incorrect datalink message found!");
                } finally {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /*
     *****************************************
     * NETWORK LINK LAYER *
     *****************************************
     */

    // Send message to datalink
    public void network_receive_from_transport(String msg, int len, int destination) {
        int total_length = 0;
        StringBuilder builder = new StringBuilder();
        // Create into an appropriate message format
        {
            builder.append("D");
            builder.append(this.NODE_ID);
            builder.append(destination);
            total_length = len + 5;
            builder.append((total_length > 10) ? total_length : '0' + total_length);
            builder.append(msg);
        }
        int n_dest = NODE_ID;
        if(distance_vector[destination][2]==-1){
            // There's no path then, send to neighbour string
            System.out.println("DV cannot find a path to the destination.");
            _echlo();
            int sub = 1;
            for(int i=destination; i>0; i--){
                if(distance_vector[destination-sub++][2]!=-1){
                    n_dest = i; 
                }
            }
            if(n_dest==NODE_ID){
                for(int i=destination; i<10; i++){
                    if(distance_vector[destination-sub++][2]!=-1){
                        n_dest = i; 
                    }
                }
            }            
        }else{
            n_dest = distance_vector[destination][2];
        }
        // System.out.println("Sending to: "+ n_dest);
        datalink_receive_from_network(builder.toString(), total_length, n_dest);
    }

    // Get message from datalink
    public void network_receive_from_datlink(String msg, int length_msg, int destination) {
        if (msg.charAt(0) == 'D') { // Data message
            // Case when message is intended to you
            int calculated_destination = Integer.valueOf(String.valueOf(msg.charAt(2)));
            if (calculated_destination == this.NODE_ID) {
                // Check the length of the message
                String length = String.valueOf(msg.subSequence(3, 5));
                if (length.equals(Integer.valueOf(length_msg).toString())) {
                    // Decrypt the message and send to transport layer
                    transport_receive_from_network(String.valueOf(msg.subSequence(5, msg.length())));
                } else {
                    System.err.println("Length does not match " + length + " " + length_msg);
                }
            } else { // Message is not for you
                // Check if you can send the message out
                if (distance_vector[calculated_destination][2] == -1) {
                    // Drop the message
                    System.err.println("Network layer --> Message is dropped. No route found");
                    return;
                }
                System.out.printf("Network Layer ----> Transferring message to new node:  %s from:%d to: %d", msg, destination ,distance_vector[calculated_destination][2]);
                datalink_receive_from_network(msg, length_msg, distance_vector[calculated_destination][2]);
            }
        } else {
            // Distance Vector
            Integer source_of_packet = Integer.valueOf(String.valueOf(msg.charAt(1))); // Who send the DV packet
            if (source_of_packet != NODE_ID) {
                String[] cost_of_other_node = msg.substring(2, msg.length()).split(",");
                Integer cost_to_sender = distance_vector[source_of_packet][1];
                // Check if current cost is higher than new cost
                for (int i = 0; i < cost_of_other_node.length; i++) {
                    Integer cost_of_node = Integer.valueOf(cost_of_other_node[i]);
                    int total_cost = cost_of_node + cost_to_sender;
                    if (total_cost < distance_vector[i][1]) {
                        // Update the cost
                        distance_vector[i][1] = total_cost;
                        // CHange next hop router
                        distance_vector[i][2] = source_of_packet;
                    }
                }
            }
        }
    }

    // Send Distance Vector
    public void network_send_dv() {
        StringBuilder builder = new StringBuilder();
        builder.append('R');
        builder.append(this.NODE_ID);
        for (int i = 0; i < 10; i++) {
            builder.append(distance_vector[i][1]);
            builder.append(',');
        }
        for (int i = 0; i < 10; i++) {
            // Send packet if cost is 1
            if (distance_vector[i][1] == 1) {
                datalink_receive_from_network(builder.toString(), builder.length(), i);
            }
        }
    }

    /*
     *****************************************
     * TRANSPORT LAYER *
     *****************************************
     */

    // Send String
    public void transport_send_string(String msg) {
        String split_messages[] = msg.split("(?<=\\G.{5})");
        if (split_messages[split_messages.length - 1].length() < 5) {
            for (int i = split_messages[split_messages.length - 1].length(); i < 5; i++) {
                split_messages[split_messages.length - 1] += ' ';
            }
        }
        if (transport_send_string_loop == split_messages.length) {
            return;
        }
        if (transport_send_XOR) {
            // Format the message
            StringBuilder builder = new StringBuilder();
            {
                builder.append('r');
                builder.append(this.NODE_ID);
                builder.append(this._destination);
                builder.append((transport_XOR_sequence > 10) ? transport_XOR_sequence
                        : "0" + String.valueOf(transport_XOR_sequence));
                builder.append(transport_XOR_message[transport_XOR_sequence]); // APpend the body here
            }
            ++transport_XOR_sequence;
            // Send to network layer to send message
            network_receive_from_transport(builder.toString(), builder.length(), this._destination);
            if (transport_XOR_message.length == transport_XOR_sequence) {
                transport_send_XOR = false;
                return;
            }
        } else {
            // Format the message
            StringBuilder builder = new StringBuilder();
            {
                builder.append('d');
                builder.append(this.NODE_ID);
                builder.append(this._destination);
                builder.append((transport_send_string_loop >= 10) ? transport_send_string_loop
                        : '0' + Integer.toString(transport_send_string_loop));
                builder.append(split_messages[transport_send_string_loop]);
            }
            ++transport_send_string_loop;
            if (transport_send_string_loop % 2 == 0) {
                String first = split_messages[transport_send_string_loop - 1];
                String second = split_messages[transport_send_string_loop - 2];
                String xorBuilder = "";
                for (int i = 0; i < first.length(); i++) {
                    char c = (char) (first.charAt(i) ^ second.charAt(i));
                    xorBuilder += c;
                }
                Base64.Encoder encoder = Base64.getEncoder();
                String encoded_msg = encoder.encodeToString(xorBuilder.getBytes());
                transport_XOR_message = encoded_msg.split("(?<=\\G.{5})");
                transport_XOR_sequence = 0;
                transport_send_XOR = true;
            }
            network_receive_from_transport(builder.toString(), builder.length(), this._destination);
        }
    }

    // Receive a transport layer message
    public void transport_receive_from_network(String mString) {
        // Regular message case
        if ("d".equals(String.valueOf(mString.charAt(0)))) {
            msg_translate.put(mString, 0);
        } else { // XOR CASE
            XOR_msg_translate.put(mString, 0);
        }
    }

    // Reassamble the string and output the string
    public void transport_output_all_received() {
        ArrayList<String> all_messages = new ArrayList<String>();
        // Create a 2d Array list for getting from multiple source
        ArrayList<ArrayList<String>> D_message = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < 10; i++) {
            D_message.add(new ArrayList<String>());
        }
        // Separate the messages into separate group
        for (String messString : msg_translate.keySet()) {
            Integer source = Integer.valueOf(String.valueOf(messString.charAt(1)));
            D_message.get(source).add(messString);
        }
        {
            for (ArrayList<String> sourceArrayList : D_message) {
                if (sourceArrayList.size() != 0) {
                    int previous = -1;
                    String previous_sString = null;
                    StringBuilder builder = new StringBuilder();
                    int i = 0;
                    for (String string : sourceArrayList) {
                        Integer current = Integer.valueOf(String.valueOf(string.subSequence(3, 5)));
                        if (current == (previous + 1)) {
                            // Add the message if the sequence matches
                            builder.append(String.valueOf(string.subSequence(5, string.length())));
                            previous = current;
                        } else if (current == 0) { // Reset the loop
                            // Add the current string
                            all_messages.add(builder.toString());
                            // Clear the string builder
                            builder.delete(0, builder.length());
                            // Add the new string
                            builder.append(String.valueOf(string.subSequence(5, string.length())));
                            previous = 0;
                        } else { // XOR case! Not working!!
                            System.err.println("Missing messages");
                            previous = current;
                            int odd_number = i % 2 == 0 ? i + 1 : i;
                            int even_number = odd_number - 1;
                            int XOR_sequence_number = even_number / 2;
                            ArrayList<String> convert = new ArrayList<String>();
                            for (String xor_mString : XOR_msg_translate.keySet()) {
                                convert.add(xor_mString);
                            }
                            if (XOR_sequence_number < convert.size()) {
                                System.out.println(convert.get(XOR_sequence_number));
                                Base64.Decoder decoder = Base64.getDecoder();
                                byte[] res = decoder.decode(convert.get(XOR_sequence_number));
                                String decoder_key = new String(res);
                                System.out.println(decoder_key+" "+decoder_key.length());
                                String resolvedString = "";
                                if (previous_sString != null) {
                                    int len = decoder_key.length() < previous_sString.length() ? decoder_key.length()
                                            : previous_sString.length();
                                    for (int j = 0; j < len; j++) {
                                        char c = (char) (previous_sString.charAt(j) ^ decoder_key.charAt(j));
                                        resolvedString += String.valueOf(c);
                                    }
                                }
                                System.out.println("Resolved string by XOR: " + resolvedString);

                            }
                            // Use these numbers to get the XOR value

                        }
                        ++i;
                        previous_sString = string;
                    }
                    all_messages.add(builder.toString());
                }
            }
        }

        for (String string : all_messages) {
            System.out.println(string);
        }
    }

    // Instantiate the app
    public App(int id, int[] neigh, int life, int destination, String msg, int wait_time) throws InterruptedException {
        NODE_ID = id;
        int start = wait_time;
        _msg = msg;
        _destination = destination;

        // Initially the next hop nodes is -1
        // and cost is inifinity
        for (int i = 0; i < 10; i++) {
            distance_vector[i][0] = i;
            distance_vector[i][1] = 999;
            distance_vector[i][2] = -1;
        }
        
        for (int i : neigh) { // Initialize the neighbours with a DV of one
            distance_vector[i][1] = 1; // The cost to reach neighbours is 1
            distance_vector[i][2] = i; // The next hop router is the next router itself
        }

        // Manipulate dv table for self
        distance_vector[NODE_ID][1] = 0;
        distance_vector[NODE_ID][2] = NODE_ID;

        for (int i = 0; i < life; i++) {
            datalink_receive_from_channel();
            network_send_dv();
            if (i >= start) {
                transport_send_string(msg);
            }            
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("Message received is: ");
        transport_output_all_received();
    }

    public void _echlo() {
        System.out.printf("id:%d\ndest:%d\nmsg:%s\n", this.NODE_ID, this._destination, this._msg);
        System.out.println("Distance Vector table");
        for (int i = 0; i < distance_vector.length; i++) {
            System.out.println(distance_vector[i][0]+" "+distance_vector[i][1]+" "+distance_vector[i][2]);
        }
    }

    // Case when there is no message to send
    public App(int id, int[] neigh, int life, int destination) throws InterruptedException {
        // Handle special case
        NODE_ID = id;
        _destination = 100+destination;

        // Initially the next hop nodes is -1
        // and cost is inifinity
        for (int i = 0; i < 10; i++) {
            distance_vector[i][0] = i;
            distance_vector[i][1] = 999;
            distance_vector[i][2] = -1;
        }

        for (int i : neigh) { // Initialize the neighbours with a DV of one
            distance_vector[i][1] = 1; // The cost to reach neighbours is 1
            distance_vector[i][2] = i; // The next hop router is the next router itself
        }

        // Manipulate dv table for self
        distance_vector[NODE_ID][1] = 0;
        distance_vector[NODE_ID][2] = NODE_ID;

        
        
        for (int i = 0; i < life; i++) {
            datalink_receive_from_channel();
            network_send_dv();
            // Since there is no message to send
            // if (i >= start) {
            // transport_send_string(msg);
            // }
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("Message received is: ");
        transport_output_all_received();
    }

    /*
     *****************************************
     * DRIVER *
     *****************************************
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No arguments");
            System.exit(1);
        }
        int wait_time = 0;
        String msg = "";
        ArrayList<Integer> neighbourIntegers = new ArrayList<Integer>();
        if (args.length > 5) {
            for (int i = 5; i < args.length; i++) {
                neighbourIntegers.add(Integer.valueOf(args[i]));
            }

        }

        // Add all the integers
        int[] neighbours = new int[neighbourIntegers.size()];
        int index = 0;
        for (int i : neighbourIntegers) {
            neighbours[index++] = i;
        }

        if (args[2].equals(args[0])) { // Case when there is no message to send
            int[] n = new int[args.length - 3];
            for (int i = 3; i < args.length; i++) {
                n[i - 3] = Integer.valueOf(args[i]);
                System.out.println(args[0] + " Node: " + i);
            }
            App app = new App(Integer.valueOf(args[0]), // ID of the node
                    n, // List of neighbours
                    Integer.valueOf(args[1]), // Life time of the node
                    Integer.valueOf(args[2]) // destination of data
            );
        } else {
            wait_time = Integer.valueOf(args[4]);
            msg = args[3];
            msg = args[3];
            App app = new App(Integer.valueOf(args[0]), // ID of the node
                    neighbours, // List of neighbours
                    Integer.valueOf(args[1]), // Life time of the node
                    Integer.valueOf(args[2]), // destination of data
                    msg, // msg
                    wait_time // Wait time
            );
        }
    }
}


// code java App 0 100 0 1 2 3 4 > 0.txt & java App 4 100 4 0 > 4.txt & java App 1 100 4 "From1" 20 0 > 1.txt & java App 2 100 4 "From2" 20 0 > 2.txt & java App 3 100 4 "From3" 20 0 >3.txt &
// java App 0 100 5 "MSG" 20 1 2 & > 0.txt java App 1 100 1 0 2 > 1.txt & java App 2 100 2 0 1 3 4 > 2.txt & java App 3 100 3 2 4 > 3.txt & java App 4 100 4 2 3 5 > 4.txt & java App 5 100 5 4 > 5.txt &
