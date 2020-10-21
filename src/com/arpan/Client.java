package com.arpan;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    public static final int PORT_NUM = 8000;

    PeerConnection requestSocket;    //socket connect to the server
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;           //stream read from the socket
    String message;                 //message send to the server
    String MESSAGE;                 //capitalized message read from the server

    String peerId;

    public Client(String peerId) {
        this.peerId = peerId;
    }

    void run()
    {
        try{
            //create a socket to connect to the server
            Socket socket = new Socket("localhost", PORT_NUM);
            requestSocket = new PeerConnection(socket);
            System.out.println("Connected to localhost in port " + PORT_NUM);

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getSocket().getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getSocket().getInputStream());

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("Hello, please input a sentence: ");
                //read a sentence from the standard input
                message = bufferedReader.readLine();
                //Send the sentence to the server
                sendMessage(message);
                //Receive the upperCase sentence from the server
                MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    //send a message to the output stream
    void sendMessage(String msg)
    {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    //main method
    public static void main(String[] args)
    {
        Client client = new Client(args[0]);
        client.run();
    }

}
