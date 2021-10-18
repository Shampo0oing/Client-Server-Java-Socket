import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.net.ConnectException;

public class Client
{
	private static Socket socket;
	
	private static boolean serverAddressVerification(String serverAddress)
	{	
		
		/*
		 * Fonction qui vérifie si l'adresse ip donnée est correcte.
		 */
		
		// On vérifie d'abord que l'adresse ne commence ni ne termine pas par un point.
		if (serverAddress.startsWith(".") || serverAddress.endsWith("."))
		{
			return false;
		}
		
		// On divise l'adresse par rapport aux points.
		String[] serverAddressParts = serverAddress.split("\\.");
		int nbr_parts = serverAddressParts.length;
		
		if (nbr_parts != 4)
		{
			return false; // il n'y a pas 4 octects
		} else
		{
			// Pour chaque octet on vérifie que c'est un nombre entre 0 et 255.
			for (int i=0; i<4; i++)
			{
				try
				{
					int number = Integer.parseInt(serverAddressParts[i]);
					if (number <0 || number >255) 
					{
						return false;
					}
				} 
				catch (NumberFormatException e) 
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/*
	 * Application client
	 */
	public static void main(String[] args) throws Exception
	{
		
		// Adresse du port et serveur
		String serverAddress = null;
		int serverPort = 0;
		socket = new Socket();; // on initialise le serveur ici car on va s'assurer que la connection se fait correctement.
		Boolean socketConnected = false;
		
		// Scanner pour lire les commandes entrées par le client
		Scanner reader = new Scanner(System.in);
		
		// ############ Partie 1 connection client et serveur ############
		
		while (!socketConnected) 
		{
			// On demande une adresse serveur et on vérifie la validité.
			Boolean validServerAddress = false;
			
			while (!validServerAddress)
			{
				
				System.out.println("Enter server address:");
				serverAddress = reader.nextLine();
				
				validServerAddress = serverAddressVerification(serverAddress);
				
				if (!validServerAddress)
				{
					System.out.println("Invalid server address, please respect the following format: \"x.x.x.x\" with x between 0 and 255");
				}
			}
			
			// On demande un port et on vérifie la validité.
			Boolean validPort = false;
			
			while (!validPort)
			{
				System.out.println("Enter port number:");
				try
				{
					serverPort = Integer.parseInt(reader.nextLine());
					System.out.println("Server port entered is " + serverPort);
					
					if (serverPort >= 5002 && serverPort <= 5049)
					{
						validPort = true;
					} else
					{
						System.out.println("Invalid number, please enter a port between 5002 and 5049");
					}
					
				} catch (NumberFormatException e) 
				{
					System.out.println("Invalid server port, please enter a port between 5002 and 5049");
				}
			}
			
			try 
			{
				// Création d'une nouvelle connexion avec le serveur
				socket = new Socket(serverAddress, serverPort);
				socketConnected = true;
			}
			catch (ConnectException e)
			{
				System.out.format("There is no server listening on %s:%d%n", serverAddress, serverPort);
				System.out.println("Do you wish to try again?[y/n]");
				String tryAgain = null;
				while (tryAgain == null)
				{
					tryAgain = reader.nextLine();
					if (tryAgain.equals("y") || tryAgain.equals("Y"))
					{
						socketConnected = false;
					} else 
					{
						if (tryAgain.equals("n") || tryAgain.equals("N"))
						{
							reader.close();
							System.exit(0);
						} else
						{
							System.out.println("Please, press either y to try again or n ro exit.[y/n]");
							tryAgain = null;
						}
					}
				}
			}
		}
		
		// ############ Partie 2 Interaction avec le serveur ############
		
		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
		
		// Création d'un canal entrant pour recevoir les messages envoyés par le serveur
		DataInputStream in = new DataInputStream(socket.getInputStream());
		
		// Création d'un canal sortant pour envoyer des messages au serveur
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		
		// Attente de la réception d'un message envoyé par le serveur sur le canal
		String helloMessageFromServer = in.readUTF();
		System.out.println(helloMessageFromServer);
		
		while (true)
		{
			// Pour l'instant y'a juste "write a command" mais on peut changer genre afficher le chemin actuel ou quoi si vous préférez.
			System.out.print("write a command:");
			String command = reader.nextLine();
			
			if (command.equals("exit")) 
			{
				// Fermeture de la connexion avec le serveur et du scanner.
				out.writeUTF(command);
				System.out.println("Closing time");
				reader.close();
				socket.close();
				break;
			} else
			{
				// At socket creation it sends an empty command. 
				// This is handled here, we just go to the next iteration of the wile loop
				if (command.equals("")) {continue;} 
				
				// On prend que le premier mot de la commande.
				String commandFirst = command.split(" ")[0];
				
				switch(commandFirst) {
				case "cd":
					
					//System.out.println("sending cd " + command);
					
					out.writeUTF(command);
					String cdAnswer = in.readUTF();
					if (!cdAnswer.equals("success"))
					{
						// Si cd n'a pas fonctionné, on affiche un message d'erreur. Sinon, on affiche rien.
						System.out.println(cdAnswer);
					}
					break;
					
				case "ls":
					
					//System.out.println("sending ls");
					
					out.writeUTF(command);
					String lsAnswer = in.readUTF();
					System.out.println(lsAnswer);
					break;
					
				default:
					System.out.println("command " + command + " does not exist");
				}
			}
		}
		
	}
}