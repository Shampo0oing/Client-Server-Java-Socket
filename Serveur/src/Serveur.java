import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Serveur 
{
	private static ServerSocket listener;
	
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
	 * Application Serveur
	 */
	public static void main(String[] args) throws Exception
	{
		// dossier root du serveur.
		final String directory = System.getProperty("user.dir");
		
		// Compteur incrémenté à chaque connexion d'un client au serveur
		int clientNumber=0;
		
		// Adresse et port du serveur
		String serverAddress = null;
		int serverPort = 0;
		
		// Scanner pour lire les commandes. Ici, il sera fermé après la demande d'adresse et de port.
		Scanner reader = new Scanner(System.in);
		
		// On demande une adresse serveur et on vérifie la validité.
		Boolean validServerAddress = false;
		
		// ############ Partie 1 connection client et serveur ############
		
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
		
		// Fermeture du scanner.
		reader.close();
		
		// Création de la connexion pour communiquer avec les clients
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		
		// Association de l'adresse et du port à la connexion
		listener.bind(new InetSocketAddress(serverIP, serverPort));
		
		System.out.format("the server is running on %s:%d%n", serverAddress, serverPort);
		
		try
		{
			/*
			 * A chaque fois qu'un nouveau client se connecte, on exécute la fonction
			 * Run() de l'objet ClientHandler.
			 */
			while (true)
			{
				// Important : la fonction accept() est bloquante : attend qu'un prochain client se connecte
				// Une nouvelle connexion: on incrémente le compteur clientNumber
				new ClientHandler(listener.accept(), clientNumber++, directory).start();
			}
		}
		finally
		{
			// Fermeture de la connexion
			listener.close();
		}
		
	}
	
	/*
	 * un thread qui se charge de traiter la demande de chaque client
	 * sur un socket particulier
	 */
	
	private static class ClientHandler extends Thread
	{
		private Socket socket;
		private int clientNumber;
		private String currentDirectory;
		final String rootDirectory;
		
		public ClientHandler(Socket socket, int clientNumber, String directory)
		{
			this.socket = socket;
			this.clientNumber = clientNumber;
			this.currentDirectory = directory; // Au début le dossier courant est le dossier root.
			this.rootDirectory = directory;
			System.out.println("New connection with client#" + clientNumber + " at " + socket);
		}
		
		/*
		 * un thread qui se charge d'envoyer au client un message de bienvenue
		 */
		public void run()
		{
			try 
			{
				// Création d'un canal sortant pour envoyer des messages au client
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				
				// Envoi d'un message au client
				out.writeUTF("Hello from server - you are client#" + clientNumber);
				
				// Création d'un canal entrant pour recevoir les messages du client
				DataInputStream in = new DataInputStream(socket.getInputStream());
								
				while (true) {
					try 
					{
						// lecture de la commande envoyée par le client.
						String command = in.readUTF();
						
						//System.out.println("command recieved is " + command);
						
						if (command.equals("exit"))
						{
							break; // Fermeture de la connection par le client, on va passer au finally.
						} else
						{
							// On prend que le premier mot de la commande.
							String commandFirst = command.split(" ")[0];
							switch (commandFirst) {
							case "cd":
								
								// On va changer le dossier courant.
								
								String newDirectory = currentDirectory;
								String directoryChange = command.split(" ")[1];
								String[] directoryChangeParts = directoryChange.split("/");
								String result = "success";
								
								for (int i=0; i<directoryChangeParts.length; i++)
								{
									if (directoryChangeParts[i].equals("..")) // On veut aller au parent
									{
										
										// Si on est pas encore au dossier root, on va aller au directory parent. Si on y est on fait rien.
										if (!newDirectory.equals(rootDirectory)) 
										{
											int parentDirectoryIndex = newDirectory.lastIndexOf("/");
											newDirectory = newDirectory.substring(0, parentDirectoryIndex);
										} 
									} else // On veut aller à un directory fils.
									{
										File cdCheckDirectory = new File(newDirectory + ("/") + directoryChangeParts[i]);
										if (cdCheckDirectory.isDirectory()) // Directory exists
										{
											newDirectory = newDirectory + ("/") + directoryChangeParts[i];
										} else // Directory doesn't exist
										{
											// Affichera une erreur expliquant le premier dossier du chemin qui fait un bug.
											result = "Directory " + newDirectory + ("/") + directoryChangeParts[i] + " mentionned in path doesn't exist";
											break;
										}
									}
								}
								if (result.equals("success"))
								{
									currentDirectory = newDirectory;
								}
								out.writeUTF(result);
								
								break;
								
							case "ls":
								
								// On veut afficher tous les dossier et fichiers du dossier courant. 
								
								File lsCurrent = new File(currentDirectory);
								File[] lsResults = lsCurrent.listFiles();
								String answer = "";
								for (int i=0; i<lsResults.length; i++)
								{
									answer = answer + lsResults[i].toString().split("/")[lsResults[i].toString().split("/").length-1] + "   ";
								}
								
								// On envoie la réponse au client.
								out.writeUTF(answer);
								
								break;
								
							default:
								System.out.println("Invalid command");
							}
						}
					}
					catch (EOFException e) 
					{
						// On arrive ici si jamais le client se coupe sans prévenir à l'aide de "exit".
						// Par exemple si on fait un ctrl+c dans le terminal. Dans ce cas, le serveur va couper la connection avec ce client.
						System.out.println("ended connection abruptly");
						break;
					}
				}
				
			} catch (IOException e)
			{
				System.out.println("Error handling client#" + clientNumber + ":" + e);
			}
			finally
			{
				try
				{
					// Fermeture de la connexion avec le client
					socket.close();
				}
				catch (IOException e)
				{
					System.out.println("Couldn't close a socket, what's going on?");
				}
				System.out.println("Connection with client#" + clientNumber + " closed");
			}
		}
	}
}










































