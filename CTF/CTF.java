
// An example class that uses the secure server socket class

import java.io.*;
import java.util.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.StringTokenizer;


public class CTF {
	private int port;
	private InetAddress host;
	
	
	
	// This is not a reserved port number
	static final int CLA_PORT = 8189;
	static final int LISTEN_2_CLIENT_PORT = 8190;
	static final String KEYSTORE = "LIUkeystore.ks";
	static final String TRUSTSTORE = "LIUtruststore.ks";
	static final String KEYSTOREPASS = "123456";
	static final String TRUSTSTOREPASS = "abcdef";
	static final String IDENTITY = "_CTF_";

	static final String GET_STATS = "STATS";
	static final String GET_CHOICES = "CHOICES";
	static final String GET_VALIDATION = "VN";
	static final String VOTE = "VOTE";
	static final String VALID = "VALID";
	private int NUMBER_OF_CHOICES;
	
	private KeyStore ks;
	private KeyStore ts;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private SSLContext sslContext;
	private SSLSocketFactory sslFact;
	
	private String query;
	private ArrayList<String> choices;
	private ArrayList<ArrayList<String>> theResult;
	
	/** Constructor
	 * @param port The port where the server
	 *    will listen for requests
	 */
	public CTF( InetAddress host, int port ) {
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
		
		/* ********** DEMOCRACY ************ */
		query = "Wich section do you want to rule LiU?";
		choices = new ArrayList<String>();
		choices.add("Medieteknik");
		choices.add("FTL");
		choices.add("Miljövetare");
		choices.add("Arbetsterapeft");
		NUMBER_OF_CHOICES = choices.size();
		
		theResult = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < NUMBER_OF_CHOICES; ++i)
		{
			theResult.add( new ArrayList<String>());
		}
		
		
	}
	private String OpenPort(int targetPort)
	{
		try {
			SSLServerSocketFactory sslServerFactory = sslContext.getServerSocketFactory();
			SSLServerSocket sss = (SSLServerSocket) sslServerFactory.createServerSocket( targetPort );
			sss.setEnabledCipherSuites( sss.getSupportedCipherSuites() );
			
			System.out.println("\n>>>> Opening port: " + targetPort);
			
			SSLSocket incoming = (SSLSocket)sss.accept();
	
			BufferedReader in = new BufferedReader( new InputStreamReader( incoming.getInputStream() ) );
			PrintWriter out = new PrintWriter( incoming.getOutputStream(), true );			
	
			String incoming_id = in.readLine();
			System.out.println(">>>> Connected to: " + incoming_id);
			
			String output = "";
			switch(incoming_id)
			{
			case "_CLIENT_":
	
				String action = in.readLine();
				switch(action)
				{
				case GET_CHOICES:
					out.println("*********************************");
					out.println(query);
					out.println("*********************************");
					for(int i = 0; i < NUMBER_OF_CHOICES; ++i)
					{
						out.println("[" + i + "] : " + choices.get(i));
					}
					out.println("*********************************");
					out.println("What is your vote?");
					
					break;
				case VOTE:
					String vote = in.readLine();
					System.out.println("Vote: '" + vote + "'");
					try 
					{
						int choice = Integer.parseInt(vote);
						if(choice < 0 || choice > NUMBER_OF_CHOICES)
						{
							System.out.println("\n>> No such choice");
							out.println("NO_SUCH_CHOICE");
							break;
						}
						
						String validation_id = in.readLine();

						System.out.println("\n>> Validating '" + validation_id + "' with CLA...");
						String ok = SendMessage(validation_id, CLA_PORT);
						
						if(ok.equals(VALID))
						{
							System.out.println("\n>> Vote valid! Adding to tally...");
							theResult.get(choice).add(validation_id);
							out.println("Your vote has been added to the tally! It is safe to close the program.");
						}
						else
						{
							System.out.println("\nERROR: Vote already used/incorrect.");
							out.println("Sorry, something went wrong with your vote. Please try again.");
						}
					}
					catch(NumberFormatException e)
					{
							
						System.out.println(e + "\nERROR: Not a number!");
						out.println("NO_SUCH_CHOICE");
					}
					
					break;
					
				case GET_STATS:
					
					out.println("A list of all candidates. Below each candidate is a list of the validation numbers that have voted for them.");
					int votesTot = 0;
					for (int i = 0; i < NUMBER_OF_CHOICES; ++i)
					{
						out.println("[" + i + "] : " + choices.get(i) + " was voted for by:");
						
						ArrayList<String> A  = theResult.get(i);
						votesTot += A.size();
						
						for (int j = 0; j < A.size(); ++j)
						{
							out.println("\t" + A.get(j));	
						}
					}
					if(votesTot == 0)
					{
						out.println("\\nNo person has yet voted. Indeterminable result.");
					}
					else
					{
						out.println("\nThe currents standings:");
						for (int i = 0; i < NUMBER_OF_CHOICES; ++i)
						{
							double d = theResult.get(i).size()*100.0 / votesTot;
							out.println(choices.get(i) + ":\t" + d + "%");
						}
						System.out.println("Currently, " + votesTot + " people have voted.");
					}
					break;
					
				default:
					
					System.out.println("ERROR: INVALID OPERATION");
					out.println("INVALID OPERATION");
				}
				break;
			default:
				out.println("INVALID ID");
					
			}
			System.out.println("\n>>>> Closing port: " + targetPort);
			out.close();
			in.close();
			sss.close();
			return output;
		}catch(IOException e)
		 {
			System.out.println( e + "\n OpenPort FAILED " );
			e.printStackTrace();
			return "_OOPS_";
		}
		
	}
	
	public String SendMessage(String message, int targetPort)
	{
		try {
		// Handshake
		SSLSocket sss =  (SSLSocket)sslFact.createSocket(host, targetPort);
		sss.setEnabledCipherSuites( sss.getSupportedCipherSuites() );
		System.out.println("\n>> SSL/TLS handshake completed");

		// Establish I/O
		BufferedReader br;
		br = new BufferedReader( new InputStreamReader( sss.getInputStream() ) );
		PrintWriter pw = new PrintWriter( sss.getOutputStream(), true );

		// Send message
		System.out.println("\n>> Sending message: " + message);
		pw.println(IDENTITY);
		pw.println(message);
		
		// Get responce
		String output = br.readLine();
		sss.close();
		return output;
		}catch( IOException x ) {
			System.out.println( x + "\nERROR: SendMessage FAILED " );
			x.printStackTrace();
			return "_OOPS_";
		}
		
		
	}
	
	/** The method that does the work for the class */
	public void run() {
		try {
			System.out.println("--------------------------------------\n"
					+ 		   "<<<<<<< CTF SERVER INITIALIZED >>>>>>>\n"
					+ 		   "--------------------------------------");
			//int x = 0;
			while(true)
			{
				//x++;
				String incoming = OpenPort(LISTEN_2_CLIENT_PORT);
				
				for (int i = 0; i < NUMBER_OF_CHOICES; ++i)
				{
					System.out.println("[" + i + "] : " + choices.get(i) + " was voted for by:");
					
					ArrayList<String> A  = theResult.get(i);
					
					for (int j = 0; j < A.size(); ++j)
					{
						System.out.println("\t" + A.get(j));	
					}
				}
			}
			
		}
		catch( Exception x ) {
			System.out.println( x );
			x.printStackTrace();
		}
	}
	
	
	/** The test method for the class
	 * @param args[0] Optional port number in place of
	 *        the default
	 */
	public static void main( String[] args ) {
		try {
		InetAddress host = InetAddress.getLocalHost();
		int port = LISTEN_2_CLIENT_PORT;
		if (args.length > 0 ) {
			port = Integer.parseInt( args[0] );
		}
		CTF addServe = new CTF( host, port );
		addServe.run();
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
	String message = in.readLine();
	String fileName = in.readLine();
	String chunk;
	
	switch(message)
	{
	// Upload File
	case "1":

		System.out.println(">>>> case 1: Upload file");
		
		// Create File
		System.out.println("Creating file: " + fileName);
		File newFile = new File(fileName);
		FileWriter writer = new FileWriter(newFile);
		BufferedWriter bw = new BufferedWriter(writer);
		
		
		while ( !(chunk = in.readLine()).equals("END_OF_FILE") )
		{
			bw.write(chunk + "\n");
		}
		
		System.out.println("EOF reached");
		bw.close();
		writer.close();
		break;
		
	// Download File
	case "2":
		System.out.println(">>>> case 2: Download file");
		
		try {
			BufferedReader inFile =new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			
			
			while ( (chunk = inFile.readLine())!= null )
			{
				System.out.println(chunk);
				out.println(chunk);
			}
			out.println("END_OF_FILE");
			System.out.println("EOF reached!");
			inFile.close();
		}
		catch (IOException io){

			System.out.println("\nERROR: No such file!\n");
			out.println("ERROR_NO_FILE");
		}
		break;
	case "3":
		System.out.println(">>>> case 3: Delete file");
		File file2die = new File(fileName);
		if(file2die.delete())
		{
			System.out.println(fileName + " deleted!");
		}
		else
		{
			System.out.println(fileName + " could not be deleted!");
		}
		break;
	}
}

*/
/* CLA SOCKET ------------------------------------------------[CLIENT]*/
/*
SSLSocketFactory sslFact = sslContext.getSocketFactory();
SSLSocket CTF2CLA =  (SSLSocket)sslFact.createSocket(host, CLA_PORT);

CTF2CLA.setEnabledCipherSuites( CTF2CLA.getSupportedCipherSuites() );
System.out.println("\n>>>> SSL/TLS handshake with CLA completed");

BufferedReader cla_in;
cla_in = new BufferedReader( new InputStreamReader( CTF2CLA.getInputStream() ) );
PrintWriter cla_out = new PrintWriter( CTF2CLA.getOutputStream(), true );

cla_out.println("yoyo");*/
/* CLIENT SOCKET --------------------------------------------[SERVER]*/
/*SSLServerSocketFactory sslServerFactory = sslContext.getServerSocketFactory();
SSLServerSocket sss = (SSLServerSocket) sslServerFactory.createServerSocket( port );
sss.setEnabledCipherSuites( sss.getSupportedCipherSuites() );

System.out.println("\n>>>> Open to: Client ");
SSLSocket incoming = (SSLSocket)sss.accept();

BufferedReader in = new BufferedReader( new InputStreamReader( incoming.getInputStream() ) );
PrintWriter out = new PrintWriter( incoming.getOutputStream(), true );			

System.out.println("\n>>>> Connected to: Client ");
System.out.println(in.readLine() + in.readLine());
out.println("Hey man!");
*/