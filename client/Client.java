// A client-side class that uses a secure TCP/IP socket

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.ssl.*;

public class Client {
	private InetAddress host;
	private int port;
	// This is not a reserved port number 
	static final int CLA_PORT = 8189; 
	static final int CTF_PORT = 8190;
	static final String KEYSTORE = "LIUkeystore.ks";
	static final String TRUSTSTORE = "LIUtruststore.ks";
	static final String KEYSTOREPASS = "123456";
	static final String TRUSTSTOREPASS = "abcdef";
	static final String IDENTITY = "_CLIENT_";
	
	static final String NO_SUCH_USR = "-1";
	static final String ALREADY_USED = "-2";
	static final String GET_VALIDATION = "VN";
	static final String GET_STATS = "STATS";
	static final String GET_CHOICES = "CHOICES";
	static final String VOTE = "VOTE";
  
	private KeyStore ks;
	private KeyStore ts;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private SSLContext sslContext;
	private SSLSocketFactory sslFact;
	
	// Constructor @param host Internet address of the host where the server is located
	// @param port Port number on the host where the server is listening
	public Client( InetAddress host, int port ) {
		this.host = host;
		this.port = port;
		try {
			ks = KeyStore.getInstance( "JCEKS" );
			ks.load( new FileInputStream( KEYSTORE ), KEYSTOREPASS.toCharArray() );
			
			ts = KeyStore.getInstance( "JCEKS" );
			ts.load( new FileInputStream( TRUSTSTORE ), TRUSTSTOREPASS.toCharArray() );
			
			kmf = KeyManagerFactory.getInstance( "SunX509" );
			kmf.init( ks, KEYSTOREPASS.toCharArray() );
			
			tmf = TrustManagerFactory.getInstance( "SunX509" );
			tmf.init( ts );
			
			sslContext = SSLContext.getInstance( "TLS" );
			sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
			sslFact = sslContext.getSocketFactory();
			
		}catch( Exception x ) {
			System.out.println( x + "\n INITIALIZATION FAILED " );
			x.printStackTrace();
		}
	}
	
	public String SendMessage(String message, int targetPort, String request)
	{
		try {
			// Handshake
			SSLSocket sss =  (SSLSocket)sslFact.createSocket(host, targetPort);
			sss.setEnabledCipherSuites( sss.getSupportedCipherSuites() );
			//System.out.println("\n>> SSL/TLS handshake completed");
	
			// Establish I/O
			BufferedReader br;
			br = new BufferedReader( new InputStreamReader( sss.getInputStream() ) );
			PrintWriter pw = new PrintWriter( sss.getOutputStream(), true );
	
			// Send message
			String toSend = IDENTITY + "\n" + request + "\n" + message;
			//System.out.println("\n>> Sending: " + toSend);
			pw.println(toSend);
			
			// Get responce
			
			String output = br.readLine();
			while(true)
			{
				String chunk = br.readLine();
				if(chunk == null) 
				{
					break;
				}
				output = output +"\n"+ chunk ;
			}
			
			sss.close();
			return output;
		}catch( IOException x ) {
			System.out.println( x + "\nERROR: SendMessage FAILED " );
			x.printStackTrace();
			return "_OOPS_";
		}

	}
	
	
  // The method used to start a client object
	public void run() {
		try 
		{
			while(true)
				
			{

				System.out.println("\n\n*********************************");
				System.out.println("What do you want to do?");
				System.out.println("*********************************");
				System.out.println("[1] : Vote \n[2] : See who has voted \n[3] : See the results");
				System.out.print("My choice : ");
				String toDo = (new BufferedReader(new InputStreamReader(System.in))).readLine();
				switch(toDo)
				{
				// Vote
				case "1":

					System.out.println("\n----------------------------------------------------------");
					System.out.println("| Enter your credentials to log in to the voting system. |");
					System.out.println("----------------------------------------------------------");
					
					System.out.print("Personal number: ");
					String usr = (new BufferedReader(new InputStreamReader(System.in))).readLine();

					System.out.print("Password: ");
					String psw = (new BufferedReader(new InputStreamReader(System.in))).readLine();
					
					String message =usr + "\n" + psw;
					
					String validation_id = SendMessage(message, CLA_PORT, GET_VALIDATION);
					if(Integer.parseInt(validation_id) == -1)
					{
						System.out.println("ERROR: There is no citizen with that personal number and password. Check your spelling!");
						
					}
					else if(Integer.parseInt(validation_id) == -2)
					{
						System.out.println("ERROR: You have already voted. You can only vote once and it is not possible to change ones vote.");
						
					}
					else
					{
						System.out.println("----------------------------------");
						System.out.println("Your ID is: '" + validation_id + "', write down this number to be able to check that the voting tally is correct. Do NOT show this number to ANYONE. Destroy it after the election.");
						System.out.println("----------------------------------");
						System.out.println(SendMessage("", CTF_PORT, GET_CHOICES));
						
						String feedback = "NO_SUCH_CHOICE";
						String vote;
						while(feedback.equals("NO_SUCH_CHOICE")) {
							System.out.print("Enter number: ");
							vote = (new BufferedReader(new InputStreamReader(System.in))).readLine();
							
							feedback = SendMessage(vote + "\n" +validation_id, CTF_PORT, VOTE);
						}

						System.out.println(feedback);
					}
						/*-----------------------------------------------*/
					break;
					
				// See who has voted
				case "2":
					System.out.println(SendMessage("", CLA_PORT, GET_STATS));
					break;
					
				// See the results
				case "3":
					System.out.println(SendMessage("", CTF_PORT, GET_STATS));
					
					break;
					
				default:
					
					System.out.println("Please enter your choice as a number. If you want to vote, write '1' and press enter, if you want to see who voted, write '2' and press enter, if you want to see the results, write '3' and enter. ");
						
				
				

				}
			}
			
			
			//System.out.println(SendMessage(validation_id, CTF_PORT));
			
		}
		catch( Exception x ) {
			System.out.println( x );
			x.printStackTrace();
		}
	}
	
	
	// The test method for the class @param args Optional port number and host name
	public static void main( String[] args ) {
		try {
			InetAddress host = InetAddress.getLocalHost();
			int port = CLA_PORT;
			if ( args.length > 0 ) {
				port = Integer.parseInt( args[0] );
			}
			if ( args.length > 1 ) {
				host = InetAddress.getByName( args[1] );
			}
			Client addClient = new Client( host, port );
			addClient.run();
		}
		catch ( UnknownHostException uhx ) {
			System.out.println( uhx );
			uhx.printStackTrace();
		}
	}
}
/*
while(true)
{
	System.out.print("Send Message: ");
	cla_out.println((new BufferedReader(new InputStreamReader(System.in))).readLine());
}*/

/*
String input = "dummy";
while(!input.equals("0"))
{
	String fileName;
	String chunk;
	switch(input)
	{
	case "1":
		System.out.print("Document to upload: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		try {
			BufferedReader inFile =new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			socketOut.println("1");
			socketOut.println(fileName);
			
			while ( (chunk = inFile.readLine())!= null )
			{
				System.out.println(chunk);
				socketOut.println(chunk);
			}
			socketOut.println("END_OF_FILE");
			inFile.close();
		}
		catch (IOException io){

			socketOut.println("END_OF_FILE");
			System.out.println("\nERROR: No such file!\n");
			
		}
		
		//socketOut.close();
		break;
	case "2":
		System.out.print("Document to download: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		socketOut.println("2");
		socketOut.println(fileName);

		System.out.println("Creating file: " + fileName);
		File newFile = new File(fileName);
		FileWriter writer = new FileWriter(newFile);
		BufferedWriter bw = new BufferedWriter(writer);

		
		while ( !(chunk = socketIn.readLine()).equals("END_OF_FILE") )
		{
			if(chunk.equals("ERROR_NO_FILE"))
			{
				System.out.println("No such file exists on server!");
				newFile.delete();
				break;
			}
			System.out.println(chunk);
			bw.write(chunk + "\n");
		}
		bw.close();
		writer.close();
		System.out.println("EOF reached");
		
		break;
	case "3":
		System.out.print("Document to delete: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		socketOut.println("3");
		socketOut.println(fileName);
		break;
		
	}
	System.out.print("---------------------\n");
	System.out.println("What do you want to do?\n 1: Send document \n 2: Get Document \n 3: Delete Document\n 0: Exit \n");
	input = (new BufferedReader(new InputStreamReader(System.in))).readLine();

}

*/


//String numbers = "1.2 3.4 7.7";
//System.out.println( ">>>> Sending the numbers " + numbers+ " to SecureAdditionServer" );
//socketOut.println( numbers );
/*
while(true)
{
	System.out.print("Send Message: ");
	cla_out.println((new BufferedReader(new InputStreamReader(System.in))).readLine());
}*/

/*
String input = "dummy";
while(!input.equals("0"))
{
	String fileName;
	String chunk;
	switch(input)
	{
	case "1":
		System.out.print("Document to upload: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		try {
			BufferedReader inFile =new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			socketOut.println("1");
			socketOut.println(fileName);
			
			while ( (chunk = inFile.readLine())!= null )
			{
				System.out.println(chunk);
				socketOut.println(chunk);
			}
			socketOut.println("END_OF_FILE");
			inFile.close();
		}
		catch (IOException io){

			socketOut.println("END_OF_FILE");
			System.out.println("\nERROR: No such file!\n");
			
		}
		
		//socketOut.close();
		break;
	case "2":
		System.out.print("Document to download: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		socketOut.println("2");
		socketOut.println(fileName);

		System.out.println("Creating file: " + fileName);
		File newFile = new File(fileName);
		FileWriter writer = new FileWriter(newFile);
		BufferedWriter bw = new BufferedWriter(writer);

		
		while ( !(chunk = socketIn.readLine()).equals("END_OF_FILE") )
		{
			if(chunk.equals("ERROR_NO_FILE"))
			{
				System.out.println("No such file exists on server!");
				newFile.delete();
				break;
			}
			System.out.println(chunk);
			bw.write(chunk + "\n");
		}
		bw.close();
		writer.close();
		System.out.println("EOF reached");
		
		break;
	case "3":
		System.out.print("Document to delete: ");
		fileName = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		socketOut.println("3");
		socketOut.println(fileName);
		break;
		
	}
	System.out.print("---------------------\n");
	System.out.println("What do you want to do?\n 1: Send document \n 2: Get Document \n 3: Delete Document\n 0: Exit \n");
	input = (new BufferedReader(new InputStreamReader(System.in))).readLine();

}

*/


//String numbers = "1.2 3.4 7.7";
//System.out.println( ">>>> Sending the numbers " + numbers+ " to SecureAdditionServer" );
//socketOut.println( numbers );