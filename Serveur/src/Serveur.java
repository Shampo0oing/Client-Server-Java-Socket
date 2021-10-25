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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;  
import java.net.BindException;

public class Serveur 
{
	private static ServerSocket listener;
	
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
	 * cdCommand() change de dossier en fonction de l argument. Si le dossier ou on veut aller n existe pas on retourne un message d erreur au client qui l affichera.
	 * directoryChange est l argument i.e. c est le dossier ou on veut aller.
	 * newDirectory est dossier courant (ou on se trouve) cote serveur.
	 * rootDirectory est le dossier racine du serveur.
	 * pathSeparator est le caractere de separation d un chemin (\ en windows et / pour le reste)
	 */
	private static String cdCommand(String directoryChange, String currentDirectory, String rootDirectory, String pathSeparator)
	{
		String[] directoryChangeParts = directoryChange.split(pathSeparator);
		String result = "success";
		
		for (int i=0; i<directoryChangeParts.length; i++)
		{
			if (directoryChangeParts[i].equals("..")) // On veut aller au parent
			{
				
				// Si on est pas encore au dossier root, on va aller au directory parent. Si on y est on fait rien.
				if (!currentDirectory.equals(rootDirectory)) 
				{
					int parentDirectoryIndex = currentDirectory.lastIndexOf(pathSeparator);
					currentDirectory = currentDirectory.substring(0, parentDirectoryIndex);
				} 
			} else // On veut aller à un directory fils.
			{
				File cdCheckDirectory = new File(currentDirectory + pathSeparator + directoryChangeParts[i]);
				if (cdCheckDirectory.isDirectory()) // Directory exists
				{
					currentDirectory = currentDirectory + pathSeparator + directoryChangeParts[i];
				} else // Directory doesn't exist
				{
					// Affichera une erreur expliquant le premier dossier du chemin qui fait un bug.
					result = "Directory " + currentDirectory + pathSeparator + directoryChangeParts[i] + " mentionned in path doesn't exist (using cd command)";
					break;
				}
			}
		}
		if (result.equals("success"))
		{
			return currentDirectory;
		} else
		{
			return result;
		}
	}
	
	/*
	 * downloadFile() s occupe d envoyer un fichier (au format zip ou non) au client
	 * file est le nom du fichier qu il faut envoyer
	 * downloadDirectory est le dossier dans lequel se trouve le fichier a envoyer
	 * out est le stream sur lequel envoyer le fichier
	 * zipped est true si il faut compresser le fichier. False si il faut envoyer le fichier tel qu il est
	 * pathSeparator est le caractere de separation d un chemin (\ en windows et / pour le reste)
	 */
	private static void downloadFile(String file, String downloadDirectory, DataOutputStream out, boolean zipped, String pathSeparator)
	{
		
		if (zipped) 
		{
			try
			{
				// Grandement inspire d un contenu internet (https://www.baeldung.com/java-compress-and-uncompress)
				// On va d abord creer le fichier compresse cote serveur puis le transferer cote client puis le supprimer cote serveur.
				String sourceFile = downloadDirectory + pathSeparator + file;
				String fileZIP = downloadDirectory +pathSeparator + file.split("\\.")[0] + ".zip";
		        FileOutputStream fos = new FileOutputStream(fileZIP);
		        ZipOutputStream zipOut = new ZipOutputStream(fos);
		        File fileToZip = new File(sourceFile);
		        FileInputStream fis = new FileInputStream(fileToZip);
		        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
		        zipOut.putNextEntry(zipEntry);
		        byte[] bytes = new byte[1024];
		        int length;
		        while((length = fis.read(bytes)) >= 0) {
		            zipOut.write(bytes, 0, length);
		        }
		        zipOut.close();
		        fis.close();
		        fos.close();
		        
		        // On transmet le fichier zip cree du cote client.
		        File myFile = new File(fileZIP);
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
				
				// On va supprimer le fichier zip cree cote serveur.
				myFile.delete();
		        
			} catch(IOException e)
			{
				System.out.println("IOException\n" + e); // Should never happen as we already verified the file exists
			}
		} else 
		{
			try 
			{
				// Ici on transmet simplement le fichier au client
				File myFile = new File(downloadDirectory + pathSeparator + file);
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
				System.out.println("IOException\n" + e); // Should never happen as we already verified the file exists
			}
		}
		
	}
	
	/*
	 * deleteDirectory() est appele lorsque l on veut supprimer un dossier non vide. Vide le contenu du dossier puis le dossier lui meme
	 * path est le chemin qui mene au dossier que l on veut supprimer
	 */
	private static void deleteDirectory(String path)
	{
		File deleteDirectory = new File(path);
		File[] allUnderlyingFiles = deleteDirectory.listFiles();
		
		for (int i = 0; i<allUnderlyingFiles.length; i++)
		{
			if (!allUnderlyingFiles[i].delete())
			{
				deleteDirectory(allUnderlyingFiles[i].toString()); // on est tombe sur un nouveau dossier non vide donc on appelle la fonction a nouveau.
				allUnderlyingFiles[i].delete();
			}
		}
	}
	
	/*
	 * Application Serveur
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
		
		// dossier root du serveur.
		final String directory = System.getProperty("user.dir");
		
		// Compteur incrémenté à chaque connexion d'un client au serveur
		int clientNumber=0;
		
		// Adresse et port du serveur
		String serverAddress = null;
		int serverPort = 0;
		
		// Scanner pour lire les commandes. Ici, il sera fermé après la demande d'adresse et de port.
		Scanner reader = new Scanner(System.in);
		
		// ############ Partie 1 connection client et serveur ############
		
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
		
		// Fermeture du scanner.
		reader.close();
		
		// Création de la connexion pour communiquer avec les clients
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		
		// Association de l'adresse et du port à la connexion
		try
		{
			listener.bind(new InetSocketAddress(serverIP, serverPort));
		} catch(BindException e) // Peut arriver si on donne n importe quelle adresse au serveur
		{
			System.out.println("Couldn't assign requested address. Restart app to retry.");
			System.exit(0);
		}
		
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
				new ClientHandler(listener.accept(), clientNumber++, directory, pathSeparator).start();
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
		final String pathSeparator;
		
		public ClientHandler(Socket socket, int clientNumber, String directory, String pathSeparator)
		{
			this.socket = socket;
			this.clientNumber = clientNumber;
			this.currentDirectory = directory; // Au début le dossier courant est le dossier root.
			this.rootDirectory = directory;
			this.pathSeparator = pathSeparator;
			System.out.println("New connection with client#" + clientNumber + " at " + socket);
		}
		
		/*
		 * un thread qui se charge d'envoyer au client un message de bienvenue
		 */
		public void run()
		{
			try 
			{
				// Format d affichage de la date et de l heure.
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd @ HH:mm:ss");  
				
				// Création d'un canal sortant pour envoyer des messages au client
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				
				// Envoi d'un message au client
				out.writeUTF("Hello from server - you are client#" + clientNumber);
				
				// Création d'un canal entrant pour recevoir les messages du client
				DataInputStream in = new DataInputStream(socket.getInputStream());
								
				while (true) {
					try 
					{
						// lecture de la commande envoyee par le client.
						String command = in.readUTF();
						
						// On affiche cote serveur la commande recue avec l adresse du client et l heure
						LocalDateTime now = LocalDateTime.now();  
						System.out.format("[%s // %s] %s\n", socket.getRemoteSocketAddress(), dtf.format(now), command);
						
						
						if (command.equals("exit"))
						{
							break; // Fermeture de la connection par le client, on va passer au finally.
						} else
						{
							// On verifie d abord que le dossier dans lequel le client se trouve existe toujours
							// C est possible que ce soit pas le cas si un autre client l a supprime
							File directoryExists = new File(currentDirectory);
							if (!directoryExists.isDirectory())
							{
								currentDirectory = rootDirectory;
								out.writeUTF("Seems like another client deleted the directory you were in. You have been sent to root directory.");
								continue;
							} else
							{
								out.writeUTF("all good");
							}
							
							// On prend que le premier mot de la commande.
							String commandFirst = command.split(" ")[0];
							switch (commandFirst) {
							case "cd":
								
								// On va changer le dossier courant.
								
								String cdResult = cdCommand(command.split(" ")[1], currentDirectory, rootDirectory, pathSeparator);
								if (cdResult.startsWith("Directory "))
								{
									out.writeUTF(cdResult); // Il y a eu une erreur
								} else 
								{
									currentDirectory = cdResult; // Pas d erreur on change le dossier courant
									out.writeUTF("You are now in directory " + currentDirectory);
								}
								
								break;
								
							case "ls":
								
								// On veut afficher tous les dossier et fichiers du dossier courant. 
								
								File lsCurrent = new File(currentDirectory);
								File[] lsResults = lsCurrent.listFiles();
								String answer = "\n";
								for (int i=0; i<lsResults.length; i++)
								{
									if (lsResults[i].isDirectory())
									{
										answer = answer + "[Directory] " + lsResults[i].toString().split(pathSeparator)[lsResults[i].toString().split(pathSeparator).length-1] + "\n";
									} else
									{
										answer = answer + "[File] " + lsResults[i].toString().split(pathSeparator)[lsResults[i].toString().split(pathSeparator).length-1] + "\n";
									}
								}
								
								// On envoie la réponse au client.
								out.writeUTF(answer);
								
								break;
								
							case "mkdir":
								
								String mkdirFile = command.split(" ")[1];
								File mkdirCurrent = new File(currentDirectory);
								File[] mkdirResults = mkdirCurrent.listFiles();
								
								// On regarde d abord si le dossier existe deja
								boolean mkdirExists = false;
								for (int i=0; i < mkdirResults.length; i++)
								{
									if (mkdirFile.equals(mkdirResults[i].toString().split(pathSeparator)[mkdirResults[i].toString().split(pathSeparator).length-1]))
									{
										mkdirExists = true;
									}
								}
								
								if (mkdirExists)
								{
									// Le fichier existe on s assure aupres du client si il veut remplacer ou non.
									out.writeUTF("Directory " + mkdirFile + " already exists on the server side. Do you want to overwrite? [y/n]");
									String mkdirOverwriteAnswer = in.readUTF();
									if (mkdirOverwriteAnswer.equals("n") || mkdirOverwriteAnswer.equals("N")) 
									{
										// Le client a decide de ne pas overwrite le fichier donc mkdir ne se fait pas
										break;
									} else 
									{
										// Le client veut overwrite le dossier existant. On va commencer par le supprimer
										deleteDirectory(mkdirCurrent + pathSeparator + mkdirFile);
										File existingDirectory = new File(mkdirCurrent + pathSeparator + mkdirFile);
										if (!existingDirectory.delete())
										{
											out.writeUTF("An error occured while trying to overwrite directory" + mkdirFile);
										}
									}
								}
								
								File newMkdir = new File(mkdirCurrent + pathSeparator + mkdirFile);
								if (newMkdir.mkdir())
								{
									out.writeUTF(mkdirFile + " was successfully created");
								} else
								{
									out.writeUTF("An error occured while trying to create directory " + mkdirFile);
								}
								
								break;
							
							case "delete":
								
								// on regarde d abord si le nom est donne comme un chemin ou pas;
								String deleteName = command.split(" ")[1];
								String deleteDirectory = currentDirectory;
								
								if (deleteName.split(pathSeparator).length>1)
								{
									int parentDirectoryDelete = deleteName.lastIndexOf(pathSeparator);
									String directoryChangeDelete = deleteName.substring(0, parentDirectoryDelete);
									deleteName = deleteName.substring(parentDirectoryDelete);
									String deleteResult = cdCommand(directoryChangeDelete, currentDirectory, rootDirectory, pathSeparator);
									
									if (deleteResult.startsWith("Directory "))
									{
										out.writeUTF(deleteResult);
										break;
									} else
									{
										deleteDirectory = deleteResult;
									}
								}
								
								// A ce point on a que deleteCurrent est le dossier duquel le delete doit se faire ( que le fichier ait ete donne en chemin ou pas)
								// Aussi on a dans la variable deleteName simplement le nom du fichier donc sans chemin.
								
								// On va verifier que le fichier existe cote serveur.
								File deleteCurrent = new File(deleteDirectory);
								File[] deleteResults = deleteCurrent.listFiles();
								boolean deleteExists = false;
						
								for (int i=0; i < deleteResults.length; i++)
								{
									if (deleteName.equals(deleteResults[i].toString().split(pathSeparator)[deleteResults[i].toString().split(pathSeparator).length-1]))
									{
										deleteExists = true;
									}
								}
								
								if (!deleteExists)
								{
									out.writeUTF("File/Directory " + deleteName + " doesn't exist");
									break;
								}
								
								File deleteFile = new File(currentDirectory + pathSeparator + deleteName);
								try 
								{
									if (deleteFile.delete())
									{
										out.writeUTF(deleteName + " has been deleted");
									} else 
									{
										// Comme on est sur que le fichier existe a ce point cela veut dire qu il s agit d un dossier non vide.
										// On va donc supprimer tous les fichiers un par un puis le dossier.
										deleteDirectory(currentDirectory + pathSeparator + deleteName);
										deleteFile.delete();
										out.writeUTF(deleteName + " has been deleted as well as all underlying directories");
									}
								} catch (IOException e) 
								{
									out.writeUTF("File not found");
								}

								break;
								
							case "upload":
								
								// On regarde si le fichier existe cote serveur.
								String fileUpload = command.split(" ")[1];
								File uploadCurrent = new File(currentDirectory);
								File[] uploadResults = uploadCurrent.listFiles();
								boolean uploadexists = false;
								for (int i=0; i < uploadResults.length; i++)
								{
									if (fileUpload.equals(uploadResults[i].toString().split(pathSeparator)[uploadResults[i].toString().split(pathSeparator).length-1]))
									{
										uploadexists = true;
									}
								}
								
								if (uploadexists)
								{
									// Le fichier existe on s assure aupres du client si il veut remplacer ou non.
									out.writeUTF("File " + fileUpload + " already exists on the server side. Do you want to overwrite? [y/n]");
									String overwriteAnswer = in.readUTF();
									if (overwriteAnswer.equals("n") || overwriteAnswer.equals("N")) 
									{
										// Le client a decide de ne pas overwrite le fichier donc l upload ne se fait pas
										break;
									}
								} else
								{
									out.writeUTF("OK");
								}
								
								// Arrive ici l upload va se faire.
								
								File myFile = new File(currentDirectory + pathSeparator + fileUpload);
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

								out.writeUTF("File " + fileUpload + " has been uploaded successfully ");

								break;
								
							case "download":
								
								String fileDownload = command.split(" ")[1];
								String downloadDirectory = currentDirectory;
								
								// On regarde d abord si le fichier est donne en chemin. Si c est le cas on va considerer le chemin.
								if (fileDownload.split(pathSeparator).length > 1)
								{
									int parentDirectoryDownload = fileDownload.lastIndexOf(pathSeparator);
									String directoryChange = fileDownload.substring(0, parentDirectoryDownload);
									fileDownload = fileDownload.substring(parentDirectoryDownload+1);
									String downloadResult = cdCommand(directoryChange, currentDirectory, rootDirectory, pathSeparator);
									
									if (downloadResult.startsWith("Directory "))
									{
										out.writeUTF(downloadResult); // Il y a eu une erreur
										break;
									} else 
									{
										downloadDirectory = downloadResult; // Pas d erreur on change le dossier d ou le download doit se faire
									}
								}
								
								// A ce point on a que downloadCurrent est le dossier duquel le download doit se faire ( que le fichier ait ete donne en chemin ou pas)
								// Aussi on a dans la variable fileDownload simplement le nom du fichier donc sans chemin.
								
								// On va verifier que le fichier existe cote serveur.
								File downloadCurrent = new File(downloadDirectory);
								File[] downloadResults = downloadCurrent.listFiles();
								boolean downloadexists = false;
								for (int i=0; i < downloadResults.length; i++)
								{
									if (fileDownload.equals(downloadResults[i].toString().split(pathSeparator)[downloadResults[i].toString().split(pathSeparator).length-1]))
									{
										downloadexists = true;
									}
								}
								
								if (downloadexists)
								{
									// Le fichier existe on va donc commencer le download mais on envoie d abord au client un message disant que le download va se faire
									out.writeUTF("Prepare Download");
									if (command.split(" ")[command.split(" ").length-1].equals("-z"))
									{
										downloadFile(fileDownload, downloadDirectory, out, true, pathSeparator);
										out.writeUTF("File " + fileDownload.split("\\.")[0] + ".zip" + " has been downloaded successfully");
									} else
									{
										downloadFile(fileDownload, downloadDirectory, out, false, pathSeparator);
										out.writeUTF("File " + fileDownload + " has been downloaded successfully");
									}
								} else
								{
									// Le fichier n existe pas on va envoyer un message d erreur au client.
									out.writeUTF("File " + fileDownload + " doesn't exist in directory " + downloadCurrent);
								}
								
								
								
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
					} catch (NullPointerException e) 
					{
						out.writeUTF("Seems like another client deleted the file you were in, you have been sent to the root directory");
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






























