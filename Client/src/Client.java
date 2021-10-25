import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.net.ConnectException;
import java.io.IOException;
import java.net.SocketException;
import java.io.EOFException;
//import java.nio.file.Files;

public class Client
{
	private static Socket socket;
	
	/*
	 * serverAddressVerification() verifie que l adresse ip est correcte
	 * serverAddress est l adresse telle qu ecrite par l utilisateur
	 */
	private static boolean serverAddressVerification(String serverAddress)
	{	
		
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
	 * uploadFile() va envoyer le fichier designe au serveur
	 * file est le fichier que l on veut envoyer
	 * out est le stream sur lequel on envoie le fichier
	 * pathSeparator est le caractere de separation d un chemin (\ en windows et / pour le reste)
	 */
	private static void uploadFile(String file, DataOutputStream out, String pathSeparator)
	{
		try 
		{
			File myFile = new File(System.getProperty("user.dir") + pathSeparator + file);
			FileInputStream fileInput = new FileInputStream(myFile);
			int nbrBytes = (int) myFile.length();
			out.writeInt(nbrBytes);
			int count = 0;
			
			while (count < nbrBytes)
			{
				out.write(fileInput.read());
				out.flush();
				count = count + 1;
			}
			out.flush();
			fileInput.close();
			
		} catch (IOException e)
		{
			System.out.println("IOException\n" + e);
		}
	}
	
	/*
	 * Application client
	 */
	public static void main(String[] args) throws Exception
	{
		// en windows les chemins sont separes de \ alors qu en mac et linux ils sont separes par /
		final String pathSeparator;
		if (System.getProperty("os.name").contains("Windows"))
		{
			pathSeparator = "\\\\";
		} else
		{
			pathSeparator = "/";
		}

		
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
			try
			{
				// Demande d une commande au client
				System.out.print("write a command: ");
				String command = reader.nextLine();
				
				if (command.equals("exit")) 
				{
					// Fermeture de la connexion avec le serveur et du scanner.
					out.writeUTF(command);
					reader.close();
					in.close();
					out.close();
					socket.close();
					break;
				} else
				{
					// A la creation du socket une commande vide est envoyee 
					// Ici on va donc dire que si la commande est vide on fait rien et on passe a la prochaine iteration du while
					if (command.equals("")) {continue;} 
					
					// On prend que le premier mot de la commande.
					String commandFirst = command.split(" ")[0];
					
					switch(commandFirst) {
					case "cd":
						
						// On verifie les arguments
						if (command.split(" ").length != 2)
						{
							System.out.println("cd command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsCD = in.readUTF();
						if (!directoryExistsCD.equals("all good"))
						{
							System.out.println(directoryExistsCD);
							break;
						}
						
						String cdAnswer = in.readUTF();
						System.out.println(cdAnswer);
						break;
						
					case "ls":
						
						// On verifie les arguments
						if (command.split(" ").length != 1)
						{
							System.out.println("ls command doesn't take any arguments");
							break;
						}
						
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsLS = in.readUTF();
						if (!directoryExistsLS.equals("all good"))
						{
							System.out.println(directoryExistsLS);
							break;
						}
						
						String lsAnswer = in.readUTF();
						System.out.println(lsAnswer);
						break;
					
					case "mkdir":
						
						// On verifie les arguments
						if (command.split(" ").length != 2)
						{
							System.out.println("mkdir command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsMKDIR = in.readUTF();
						if (!directoryExistsMKDIR.equals("all good"))
						{
							System.out.println(directoryExistsMKDIR);
							break;
						}
						
						String mkdirAnswer = in.readUTF();
						if (!mkdirAnswer.endsWith("successfully created"))
						{
							// Lorsqu on est ici ca veut dire que le dossier existait deja on va demander au client si il veut overwrite
							System.out.println(mkdirAnswer);
							String mkdirOverwriteAnswer = reader.nextLine();
							
							while (!mkdirOverwriteAnswer.equals("y") && !mkdirOverwriteAnswer.equals("Y") && !mkdirOverwriteAnswer.equals("n") && !mkdirOverwriteAnswer.equals("N"))
							{
								System.out.println("Command invalid, " + mkdirAnswer);
								mkdirOverwriteAnswer = reader.nextLine();
							}
							
							out.writeUTF(mkdirOverwriteAnswer);
							if (mkdirOverwriteAnswer.equals("n") || mkdirOverwriteAnswer.equals("N"))
							{
								// Le client a decide de ne pas overwrite le fichier donc l upload ne se fait pas
								System.out.println("upload aborted");
								break;
							}
							mkdirAnswer = in.readUTF();
						}
						
						System.out.println(mkdirAnswer);
						
						break;
					
					case "delete":
						
						// On verifie les arguments
						if (command.split(" ").length != 2)
						{
							System.out.println("delete command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsDELETE = in.readUTF();
						if (!directoryExistsDELETE.equals("all good"))
						{
							System.out.println(directoryExistsDELETE);
							break;
						}
						
						String deleteAnswer = in.readUTF();
						System.out.println(deleteAnswer);
						
						break;
						
					case "upload":
						
						// On verifie les arguments
						if (command.split(" ").length != 2)
						{
							System.out.println("upload command expects only one argument");
							break;
						}
						
						// D abord on s assure que le fichier existe cote client.
						String uploadFile = command.split(" ")[1];
						File uploadCurrent = new File(System.getProperty("user.dir"));
						File[] uploadResults = uploadCurrent.listFiles();
						boolean uploadExists = false;
						for (int i=0; i < uploadResults.length; i++)
						{
							if (uploadFile.equals(uploadResults[i].toString().split(pathSeparator)[uploadResults[i].toString().split(pathSeparator).length-1]))
							{
								uploadExists = true;
							}
						}
						
						if (!uploadExists)
						{
							System.out.println("File " + uploadFile + " doesn't exist on client side");
							break;
						}
						
						// Arrive a ce point on est sur que le fichier existe donc on envoie la commande au serveur.
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsUPLOAD = in.readUTF();
						if (!directoryExistsUPLOAD.equals("all good"))
						{
							System.out.println(directoryExistsUPLOAD);
							break;
						}
						
						// on verifie avec le serveur si le fichier existe de son cote.
						String uploadOverwriteConflict = in.readUTF();
						if (!uploadOverwriteConflict.equals("OK"))
						{
							// Le fichier existe cote serveur on demande au client si il veut overwrite ou pas.
							System.out.println(uploadOverwriteConflict);
							String uploadOverwriteAnswer = reader.nextLine();
							
							while (!uploadOverwriteAnswer.equals("y") && !uploadOverwriteAnswer.equals("Y") && !uploadOverwriteAnswer.equals("n") && !uploadOverwriteAnswer.equals("N"))
							{
								System.out.println("Command invalid, " + uploadOverwriteConflict);
								uploadOverwriteAnswer = reader.nextLine();
							}
								
							out.writeUTF(uploadOverwriteAnswer);
							if (uploadOverwriteAnswer.equals("n") || uploadOverwriteAnswer.equals("N"))
							{
								// Le client a decide de ne pas overwrite le fichier donc l upload ne se fait pas
								System.out.println("upload aborted");
								break;
							}
						}
						
						// Arrive ici l upload doit se faire.
						uploadFile(uploadFile, out, pathSeparator);
						
						String uploadAnswer = in.readUTF();
						System.out.println(uploadAnswer);
						
						break;
						
					case "download":
						
						// On verifie les arguments
						if (command.split(" ").length != 2 && command.split(" ").length != 3)
						{
							System.out.println("download command expects either one or 2 arguments");
							break;
						}
						
						// On va d abord regarder si le fichier existe cote client et si il existe on demande au client si il veut l overwrite.
						
						String downloadFile = command.split(" ")[1];
						
						if (command.split(" ")[command.split(" ").length-1].equals("-z"))
						{
							int extensionBegins = downloadFile.lastIndexOf('.');
							downloadFile = downloadFile.substring(0, extensionBegins) + ".zip";
						}
						
						File downloadCurrent = new File(System.getProperty("user.dir"));
						File[] downloadResults = downloadCurrent.listFiles();
						boolean downloadExists = false;
						for (int i=0; i<downloadResults.length; i++)
						{
							if (downloadFile.split(pathSeparator)[downloadFile.split(pathSeparator).length-1].equals(downloadResults[i].toString().split(pathSeparator)[downloadResults[i].toString().split(pathSeparator).length-1]))
							{
								downloadExists = true;
							}
						}
						if (downloadExists)
						{
							// Le fichier existe deja cote client on va lui demander si il veut overwrite
							System.out.println("A file named " + downloadFile.split(pathSeparator)[downloadFile.split(pathSeparator).length-1] + " already exists in this directory. Do you want to overwrite it? [y/n]");
							String downloadOverwriteAnswer = reader.nextLine();
							while (!downloadOverwriteAnswer.equals("y") && !downloadOverwriteAnswer.equals("Y") && !downloadOverwriteAnswer.equals("n") && !downloadOverwriteAnswer.equals("N"))
							{
								System.out.println("Command Invalid\n a File named " + downloadFile + " already exists in this directory. Do you want to overwrite it? [y/n]");
								downloadOverwriteAnswer = reader.nextLine();
							}
							if (downloadOverwriteAnswer.equals("n") || downloadOverwriteAnswer.equals("N"))
							{
								// Le client veut pas overwrite donc download ne va pas se faire
								System.out.println("Download Aborted");
								break;
							}
						}
						
						// Arrive a ce point on est sur que le client veut faire le download. On va verifier cote serveur que le fichier existe.
						out.writeUTF(command);
						
						// On verifie que le dossier dans lequel se trouvait le client existe toujours. Peut ne pas etre le cas si un autre client a supprime le dossier.
						String directoryExistsDOWNLOAD = in.readUTF();
						if (!directoryExistsDOWNLOAD.equals("all good"))
						{
							System.out.println(directoryExistsDOWNLOAD);
							break;
						}
						
						String downloadAnswer = in.readUTF();
						if (downloadAnswer.startsWith("Directory ") || downloadAnswer.startsWith("File "))
						{
							System.out.println(downloadAnswer);
							break;
						}
						
						// Arrive ici on est sur que le download va se faire.
						
						File myFile = new File(downloadCurrent + pathSeparator + downloadFile.split(pathSeparator)[downloadFile.split(pathSeparator).length-1]);
						FileOutputStream fileOutput = new FileOutputStream(myFile);
						int nbrBytes = in.readInt();
						int count = 0;
						
						while(count<nbrBytes)
						{
							fileOutput.write(in.read());
							fileOutput.flush();
							count = count+1;
						}
						fileOutput.close();
						
						String downloadResponse = in.readUTF();
						System.out.println(downloadResponse);
						
						break;
						
					default:
						System.out.println("command " + command + " does not exist");
					}
				}
			} catch (SocketException e)
			{
				// Peut arriver si le serveur s arrete de maniere abrupte. On va arreter le client
				System.out.println("The server disconnected. There is no endpoint to client socket therefore the client will shut down now.");
				reader.close();
				in.close();
				out.close();
				socket.close();
				break;
			} catch (EOFException e)
			{
				// Peut arriver si le serveur s arrete de maniere abrupte. On va arreter le client
				System.out.println("The server disconnected. There is no endpoint to client socket therefore the client will shut down now.");
				reader.close();
				in.close();
				out.close();
				socket.close();
				break;
			} 
		}
	}
}
