import java.io.*;
import java.net.*;
import java.util.regex.Pattern;

public class Client {
	
	// new thread functions
	public static class newUserThread extends Thread{
		PrintWriter writeData;
		private Socket socket;
		private BufferedReader buffer;
		private String recvmsg1;
		
		public newUserThread(Socket socket){
			this.socket = socket;
		}

		public void run(){
			try{
				buffer=new BufferedReader(new InputStreamReader(System.in));
				writeData=new PrintWriter(socket.getOutputStream());
				
				while(true){
					recvmsg1 = buffer.readLine();
					writeData.println(recvmsg1);
					writeData.flush();
				}
			}catch(Exception e){
				return;
			}
		}
	}
	
	// main function
	public static void main(String args[]){
		String severIP=args[0];
		int serverPort;		
		Socket clientSocket = null;
		Thread newUser;
		
		serverPort=Integer.parseInt(args[1]);
		try{
			clientSocket=new Socket(severIP, serverPort);
		}catch(Exception e){
			return;
		}

		//New thread created		
		try{
			newUser = new newUserThread(clientSocket);
			newUser.start();
		}catch(Exception e){
			return;
		}

		//receiving data
		try{
			BufferedReader buffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			while(true){
				String recvmsg2 = buffer.readLine();
				if(recvmsg2!=null){
					System.out.println(recvmsg2);
				}
				else{
					throw new SocketException();
				}
			}

		}catch(SocketException e){
			return;
		} catch (IOException e) {// main function is wrong			
			return;
		}
	}

}
