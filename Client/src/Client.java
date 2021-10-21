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
	
	private static void uploadFile(String file, DataOutputStream out)
	{
		try 
		{
			File myFile = new File(System.getProperty("user.dir") + "/" + file);
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
				// Pour l'instant y'a juste "write a command" mais on peut changer genre afficher le chemin actuel ou quoi si vous préférez.
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
					// At socket creation it sends an empty command. 
					// This is handled here, we just go to the next iteration of the wile loop
					if (command.equals("")) {continue;} 
					
					// On prend que le premier mot de la commande.
					String commandFirst = command.split(" ")[0];
					
					switch(commandFirst) {
					case "cd":
						
						if (command.split(" ").length != 2)
						{
							System.out.println("cd command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						String cdAnswer = in.readUTF();
						System.out.println(cdAnswer);
						break;
						
					case "ls":
						
						if (command.split(" ").length != 1)
						{
							System.out.println("ls command doesn't take any arguments");
							break;
						}
						
						out.writeUTF(command);
						String lsAnswer = in.readUTF();
						System.out.println(lsAnswer);
						break;
					
					case "mkdir":
						
						if (command.split(" ").length != 2)
						{
							System.out.println("mkdir command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						String mkdirAnswer = in.readUTF();
						if (!mkdirAnswer.endsWith("successfully created"))
						{
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
						
						if (command.split(" ").length != 2)
						{
							System.out.println("delete command expects only one argument");
							break;
						}
						
						out.writeUTF(command);
						String deleteAnswer = in.readUTF();
						System.out.println(deleteAnswer);
						
						break;
						
					case "upload":
						
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
							if (uploadFile.equals(uploadResults[i].toString().split("/")[uploadResults[i].toString().split("/").length-1]))
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
						uploadFile(uploadFile, out);
						
						String uploadAnswer = in.readUTF();
						System.out.println(uploadAnswer);
						
						break;
						
					case "download":
						
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
							if (downloadFile.split("/")[downloadFile.split("/").length-1].equals(downloadResults[i].toString().split("/")[downloadResults[i].toString().split("/").length-1]))
							{
								downloadExists = true;
							}
						}
						if (downloadExists)
						{
							System.out.println("A file named " + downloadFile.split("/")[downloadFile.split("/").length-1] + " already exists in this directory. Do you want to overwrite it? [y/n]");
							String downloadOverwriteAnswer = reader.nextLine();
							while (!downloadOverwriteAnswer.equals("y") && !downloadOverwriteAnswer.equals("Y") && !downloadOverwriteAnswer.equals("n") && !downloadOverwriteAnswer.equals("N"))
							{
								System.out.println("Command Invalid\n a File named " + downloadFile + " already exists in this directory. Do you want to overwrite it? [y/n]");
								downloadOverwriteAnswer = reader.nextLine();
							}
							if (downloadOverwriteAnswer.equals("n") || downloadOverwriteAnswer.equals("N"))
							{
								System.out.println("Download Aborted");
								break;
							}
						}
						
						// Arrive a ce point on est sur que le client veut faire le download. On va verifier cote serveur que le fichier existe.
						out.writeUTF(command);
						
						String downloadAnswer = in.readUTF();
						if (downloadAnswer.startsWith("Directory ") || downloadAnswer.startsWith("File "))
						{
							System.out.println(downloadAnswer);
							break;
						}
						
						// Arrive ici on est sur que le download va se faire.
						
						File myFile = new File(downloadCurrent + "/" + downloadFile.split("/")[downloadFile.split("/").length-1]);
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
				System.out.println("The server disconnected. There is no endpoint to client socket therefore the client will shut down now.");
				reader.close();
				in.close();
				out.close();
				socket.close();
				break;
			} catch (EOFException e)
			{
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
