
// An example class that uses the secure server socket class

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.StringTokenizer;
import java.util.*;


public class CLA {
	private int port;
	// This is not a reserved port number 
	static final int CLA_PORT = 8189; 
	static final String KEYSTORE = "LIUkeystore.ks";
	static final String TRUSTSTORE = "LIUtruststore.ks";
	static final String KEYSTOREPASS = "123456";
	static final String TRUSTSTOREPASS = "abcdef";
	static final int NO_SUCH_USR = -1;
	static final int ALREADY_USED = -2;

	static final String VALID = "VALID";
	
	List<Citizen> myCitizens;
	List<ValidationNumber> VNList;
	Random rand;
	
	
	private KeyStore ks;
	private KeyStore ts;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private SSLContext sslContext;
	private SSLSocketFactory sslFact;
	/** Constructor
	 * @param port The port where the server
	 *    will listen for requests
	 */
	CLA( int port ) {
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
		myCitizens = new ArrayList<Citizen>();
		VNList = new ArrayList<ValidationNumber>();
		
		myCitizens.add(new Citizen("1111", "one"));
		myCitizens.add(new Citizen("2222", "two"));
		myCitizens.add(new Citizen("3333", "three"));
		myCitizens.add(new Citizen("4444", "four"));
		
		
		rand = new Random(System.currentTimeMillis());
		
		
	}
	
	private int GetValidationNumber(String check_usr, String check_psw)
	{
		// Search myCitizens to check if usr already has voted, if they have, return ALREADY_USED.
		// Check if there is a user with corresponding password. If not, return NO_SUCH_USR.
		
		System.out.println("Input: " + check_usr + " " + check_psw);
		for(int i = 0; i < myCitizens.size(); ++i)
		{

			Citizen C = myCitizens.get(i);
			//System.out.println("C = " + i + ": " + C.usr+ " " + C.psw);
			//System.out.println("C = " + i + ": " + check_usr.equals(C.usr)+ " " + check_psw.equals(C.psw));
			
			if (check_usr.equals(C.usr)  && check_psw.equals(C.psw))
			{
				
				if(C.Validate())
				{
					return GenerateValidationNumber();
				}
				else
				{
					return ALREADY_USED;
				}
			}
		}
		return NO_SUCH_USR;
	}
	
	private int GenerateValidationNumber()
	{
		int newVN = 0;
		boolean uniqueNumber = false;
		
		while(!uniqueNumber)
		{
			newVN = rand.nextInt(50000);
			//System.out.println(newVN + " " + uniqueNumber);
			for(int i = 0; i < VNList.size(); ++i)
			{
				if(VNList.get(i).n == newVN)
				{
					break;
				}
			}

			uniqueNumber = true;
			
		}
		
		VNList.add(new ValidationNumber(newVN));
		return newVN;
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
				
				
				switch(in.readLine())
				{
				case "VN":

					System.out.println("\n>> Get Validation Number ");

					String usr_in = in.readLine();
					String psw_in = in.readLine();
					int vn = GetValidationNumber(usr_in, psw_in);

					System.out.println(vn);
					out.println(vn);
					in.close();
					out.close();
					break;
					
				case "STATS":

					System.out.println("\n>> Get Statistics");
					out.println("A list of every elegible voter in this election. A 'x' means that that person has voted. A '_' means that that person has yet to vote.");
					for (int i = 0; i < myCitizens.size(); ++i)
					{
						out.println(myCitizens.get(i).toString());
					}
					
					break;
					
					
				default:
					out.println("ERROR: INVALID ACTION");
				}
				
				
				break;
			case "_CTF_":

				String toCTF = "NO_SUCH_ID";
				try
				{
					int id = Integer.parseInt(in.readLine());
					
					for(int i = 0; i < VNList.size(); ++i)
					{
						if(VNList.get(i).n == id )
						{
							if(VNList.get(i).Validate())
							{
								System.out.println("Valid vote!");
								toCTF = VALID;
							}
							else
							{
								System.out.println("ERROR: Vote already used!");
								toCTF = "USED";
							}
							break;
							
						}
					}
				}
				catch(NumberFormatException e)
				{
					System.out.println(e + "\nERROR: The sent ID was not in the form of an integer");
				}
				
				
				
				out.println(toCTF);
				
				break;
			default:
				out.println("INVALID ID");
					
			}
			System.out.println("\n>>>> Closing port: " + targetPort);

			in.close();
			out.close();
			sss.close();
			return output;
		}catch(IOException e)
		 {
			System.out.println( e + "\n OpenPort FAILED " );
			e.printStackTrace();
			return "_OOPS_";
		}
		
	}
	
	/** The method that does the work for the class */
	public void run() {
		try {
			System.out.println("--------------------------------------\n"
					+ 		   "<<<<<<< CLA SERVER INITIALIZED >>>>>>>\n"
					+ 		   "--------------------------------------");
			while(true)
			{
				System.out.println(OpenPort(CLA_PORT));
			}
			/* CTF SOCKET -----------------------------------------------[SERVER]*/
			/*
			SSLServerSocket open_ctf = (SSLServerSocket) sslServerFactory.createServerSocket( LISTEN_2_CTF_PORT );
			open_ctf.setEnabledCipherSuites( open_ctf.getSupportedCipherSuites() );
			
			System.out.println("\n>>>> Open to: CTF");
			SSLSocket incoming_ctf = (SSLSocket)open_ctf.accept();

			BufferedReader in_ctf = new BufferedReader( new InputStreamReader( incoming_ctf.getInputStream() ) );
			PrintWriter out_ctf = new PrintWriter( incoming_ctf.getOutputStream(), true );	

			System.out.println("\n>>>> Connected to: CTF");
			System.out.println(in_ctf.readLine());
			*/
			/* CLIENT SOCKET --------------------------------------------[SERVER]*/
			
			//incoming.close();
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
		int port = CLA_PORT;
		if (args.length > 0 ) {
			port = Integer.parseInt( args[0] );
		}
		CLA addServe = new CLA( port );
		addServe.run();
	}
}
/*String str;
while ( !(str = in.readLine()).equals("") ) {
	double result = 0;
	StringTokenizer st = new StringTokenizer( str );
	try {
		while( st.hasMoreTokens() ) {
			Double d = new Double( st.nextToken() );
			result += d.doubleValue();
		}
		out.println( "The result is " + result );
	}
	catch( NumberFormatException nfe ) {
		out.println( "Sorry, your list contains an invalid number" );
	}
}*/
/* int character;
while(true )
{
	System.out.print("??? ");
	character=in.read();

	System.out.print("!!! ");
	if(character == -1) break;

	System.out.print("*** ");
	System.out.println(character);
	bw.write(character);
}	
*/

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