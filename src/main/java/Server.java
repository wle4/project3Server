import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server{

	private Map<String, ClientThread> serverUsers = new HashMap<>();

	TheServer server;
	private Consumer<Serializable> callback;

	Server(Consumer<Serializable> call){
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){

				System.out.println("Server is running!");

				while(true) {
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
	

	class ClientThread extends Thread{
			
		
		Socket connection;
		ObjectInputStream in;
		ObjectOutputStream out;
		private String username;
			
		ClientThread(Socket s){
			this.connection = s;
		}
			
		public void run(){
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);

				while(true){
					Message usernameMsg = (Message) in.readObject();

					if(validUsername(usernameMsg.getMsg())){
						this.username = usernameMsg.getMsg();
						break;
					}
					else {

					}
				}

				serverUsers.put(username, this);
				callback.accept(username + "has joined");

				while(true){
					try {
						Message msg = (Message) in.readObject();
						handleMsg(msg);
					}
					catch(Exception e) {
						callback.accept("");
					}
				}
			}
			catch(Exception e) {
				//implement error message
				callback.accept("User failed to join");
			}


				
//			updateClients("new client on server: client #"+count);

		}//end of run

		private boolean validUsername(String username){
			return username != null && !serverUsers.containsKey(username);
		}

		private void handleMsg(Message msg){
			if(msg.getReceiver().equals("all")){
				sendAll(msg);
			}
			else {
				sendDM(msg);
			}

			callback.accept(msg.getSender() + ": " + msg.getMsg());
		}

		private void sendAll(Message msg){
			String sender = msg.getSender();
			String content = msg.getMsg();
			for(ClientThread client : serverUsers.values()){
				try{
					Message message = new Message("",sender, content);
					client.out.writeObject(message);
				}
				catch(Exception e){
					callback.accept(sender + "failed to send message to all");
				}
			}
		}

		private void sendDM(Message msg){
			String receiver = msg.getReceiver();
			String sender = msg.getSender();
			String content = msg.getMsg();

			if(serverUsers.containsKey(receiver)){
				try {
					Message message = new Message("", sender, content);
					serverUsers.get(receiver).out.writeObject(message);
				}
				catch(Exception e){
					callback.accept(sender + " failed to send DM to " + receiver);
				}
			}
			else{
				try {
					Message message = new Message(sender, "Server", ": User does not exist.");
					serverUsers.get(sender).out.writeObject(message);
				}
				catch(Exception e){
					callback.accept("ERROR ERROR ERROR");
				}
			}
		}

	}//end of client thread
}

