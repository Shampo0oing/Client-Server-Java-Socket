import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
	
	private static ServerSocket listener;
	
	public static void main(String[] args) throws Exception
	{
		//Compteur qui s'incrémente à chaque nouveau client au serveur
		int clientNumber = 0;
		
		String serverAddress = "127.0.0.1";
		int serverPort = 5002;
		
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		
		System.out.println(serverIP);
		listener.bind(new InetSocketAddress(serverIP, serverPort));
		
		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
		
		try {
			while(true)
			{
				new ClientHandler(listener.accept(), clientNumber++).start();
			}
		}
		finally
		{
			listener.close();
		}
		
	}
	
	private static class ClientHandler extends Thread
	{
		private Socket socket;
		private int clientNumber;
		
		public ClientHandler(Socket socket, int clientNumber)
		{
			this.socket = socket;
			this.clientNumber = clientNumber;
			System.out.println("New connection with client#"+ clientNumber + "at" + socket);
			
		}
		public void run()
	     {
	         try {
	             // Creation d'un canal sortant pour envoyer des messages au client
	             DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 

	             //Envoie d'un message au client
	             out.writeUTF("Bonjour du serveur - vous êtes client #" + clientNumber);


	         }
	         catch (IOException e)
	         {
	             System.out.println("Erreur concernant le client #" + clientNumber + ": " + e);
	         }
	         finally
	         {
	             try
	             {
	                 //Fermeture de la connexion avec le client
	                 socket.close();
	             }
	             catch(IOException e) {
	                 System.out.println("le socket n'a pas pu se fermer");
	             }
	             System.out.println("La connection avec client #" + clientNumber + " est fermé");
	         }
	     }
		
	}
}