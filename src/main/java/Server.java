import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server{

	private Map<String, ClientThread> serverUsers = new HashMap<>(); // mapping usernames with their respective ClientThread

	TheServer server; // instance of the actual server
	private Consumer<Serializable> callback; // currently used to just print strings into the server GUI

	Server(Consumer<Serializable> call){
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){

				System.out.println("Server is running!");

				while(true) { // tells us when a new client is trying to join
					ClientThread c = new ClientThread(mysocket.accept());
					callback.accept("User is attempting to connect...");
					c.start();
				}
			}//end of try
			catch(Exception e) {
				callback.accept("Server socket did not launch");
			}
		}//end of while
	}

	public ArrayList<String> updateCurrentUsers(){
		return new ArrayList<>(serverUsers.keySet());
	}

	class ClientThread extends Thread{

		Socket connection; // socket connection between Server and Client
		ObjectInputStream in; // socket input stream
		ObjectOutputStream out; // socket output stream

        ClientThread(Socket s){
			this.connection = s;
		}
			
		public void run(){
			try {
				//initializes socket information
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);

                String username; //individual client's username is stored here

                while(true){ // THIS WHILE LOOP IS SPECIFICALLY FOR JUST THE USERNAME, continues to loop here until
					Message usernameMsg = (Message) in.readObject(); // unpacks incoming Message objects

					if(validUsername(usernameMsg.getMsg())){ // checks if the user's username is valid
						serverMessage(new Message("", "Server", "good")); // sends back a mesage saying that the username is valid
						username = usernameMsg.getMsg();
						break; // stores username for this specific ClientThread and breaks out of the username loop
					}
					else {
						serverMessage(new Message("", "Server", "Username already taken...")); // sends message back if their username is already taken
					}
				}

				synchronized (serverUsers) { // ensures thread safety when connecting a new client to the serverUsers
					serverUsers.put(username, this); // adds the client's username and ClientThread to the serverUsers map
					signalUpdateUsers();
				}
				sendAll(new Message("", "SERVER", username + " has joined"));
				callback.accept(username + " has joined"); // prints out on the server that a new user has joined

				while(true){ // loops for checking for new Message objects coming from client
					try {
						Message msg = (Message) in.readObject();
						handleMsg(msg); // client's Message will be handled in a separate function
					}
					catch(Exception e) { // if a client disconnects
						break;
					}
				}
				callback.accept(username + " has disconnected"); // prints that this user has disconnected

				synchronized(serverUsers) { // // ensures thread safety when disconnecting an existing client from the serverUsers
					serverUsers.remove(username); // remove user from user list
					signalUpdateUsers();
				}

				sendAll(new Message("","SERVER", username + " has disconnected")); // announces to everyone that this user has disconnected
				connection.close(); // close client socket
			}
			catch(Exception e) {
				//implement error message
				callback.accept("User failed to join");
			}

		}//end of run

		private boolean validUsername(String username){
			// checks if the client's username is valid
			return username != null && !serverUsers.containsKey(username);
		}

		private void serverMessage(Message msg){
			//specific for sending a message from the Server to a specific client (used for checking username)
			try{
				out.writeObject(msg);
			}
			catch(Exception e){}
		}

		private void handleMsg(Message msg){
			// used for handling client messages
			if(msg.getReceiver().equals("all")){ // if the receiver says "all", this means to send to all clients
				sendAll(msg);
				callback.accept(msg.getSender() + ": " + msg.getMsg()); //prints client and their message in server
			}
			else { // specifically handles direct messages between clients
				sendDM(msg);
			}
		}

		private void sendAll(Message msg){
			// handles sending a message to ALL clients (for the public chatroom)
			String sender = msg.getSender(); // unpacks client's Message object
			String content = msg.getMsg();

			// for-each loop going through the serverUsers and sending a new Message object to them
			for(ClientThread client : serverUsers.values()){
				try{
					Message message = new Message("",sender, content);
					client.out.writeObject(message); // sends new Message object to client
				}
				catch(Exception e){ // server side error, something went wrong while sending to one of the clients
					callback.accept(sender + "failed to send message to all");
				}
			}
		}

		private void sendDM(Message msg){
			// unpacking the Message object from the client
			String receiver = msg.getReceiver();
			String sender = msg.getSender();
			String content = msg.getMsg();

			if(serverUsers.containsKey(receiver)){ // checks if the user exists in our map of usernames
				try {
					Message message = new Message("", sender, "whispers " + content); // repacks the Message object and sends it to the appropriate user
					serverUsers.get(receiver).out.writeObject(message);
					serverUsers.get(sender).out.writeObject(message);
					callback.accept(sender + " sent a message to " + receiver + ": " + content); // print direct messages into the server GUI to keep track of who's sending what to who
				}
				catch(Exception e){ // Server side error, message wasn't sent for whatever reason
					callback.accept(sender + " failed to send DM to " + receiver);
				}
			}
			else{ // if user is not found, send a Message object back to the sender saying they don't exist
				try {
					Message message = new Message(sender, "Server", ": User does not exist.");
					serverUsers.get(sender).out.writeObject(message);
				}
				catch(Exception e){ // server side error, error message was not able to send back to the sender
					callback.accept("E");
				}
			}
		}

		private void signalUpdateUsers(){
			ArrayList<String> userList = new ArrayList<>();
			userList = updateCurrentUsers();

			for(ClientThread client : serverUsers.values()){
				try{
					Message update = new Message("UPDATE", userList);
					client.out.writeObject(update); // sends new Message object to client
				}
				catch(Exception e){ // server side error, something went wrong while sending to one of the clients
					callback.accept("Failed to send updated Client list");
				}
			}
		}

	}//end of client thread
}

