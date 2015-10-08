import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Server {

	final static int BLOCK_MINUTE = 1;
	//block time set (minutes)
	final static int LAST_HOUR = 1;
	//wholasthr time set (hours)
	final static int TIME_OUT=30;
	//time out time (minutes)
	//static int orderNumber = 0;
	//order number remembered in server
	static boolean log = true;
	//whether a user is loged in
	static ArrayList<User> onlineUser = new ArrayList<User>();
	//users that is connecting the server
	static ArrayList<User> allUser = new ArrayList<User>();
	//users that are connected with the server in an hour
	static ArrayList<User> blockUser = new ArrayList<User>();
	//users that are blocked
	static HashMap<String, String> userRecord = new HashMap<String, String>();
	//users and passwords


	// Class to describe a user, saving username, logIn, its socket and status.
	public static class User {
		public Socket socket;
		public String userName;
		public Calendar logIn;
		public Boolean online;

		public User(String userName, Calendar logIn, Socket socket) {
			this.socket = socket;
			this.userName = userName;
			this.logIn = logIn;
			this.online = true;
		}
	}

	// read user information to a hushmap
	public static void readFile(String file, HashMap<String, String> userRecord) {
		try {
			String oneUser;
			String[] userInfo;
			
			FileReader openFile = new FileReader(file);
			BufferedReader buffer = new BufferedReader(openFile);		
			
			while ((oneUser = buffer.readLine()) != null) {
				userInfo = oneUser.split(" ");
				userRecord.put(userInfo[0], userInfo[1]);
			}
			buffer.close();
			openFile.close();
		} catch (IOException e) {
			System.out.println("exception occurs when save users");
			return;
		}
	}

	//update the active time of a certain user
	private static void userActive(User user) {
		for(int i=0;i<allUser.size();i++){
			if(allUser.get(i).userName.equals(user.userName)){
				Long inactiveDif = Calendar.getInstance().getTimeInMillis() - allUser.get(i).logIn.getTimeInMillis();
				if (inactiveDif > TIME_OUT  * 1000) {
					onlineUser.remove(user);
					try {
						user.socket.close();
					}catch (IOException e){
						e.printStackTrace();
					}
					System.out.println("Action : " + user.userName + "is above TIME_OUT and logged out ");
					return;
				}			
			allUser.get(i).logIn=Calendar.getInstance();
			}
		}
	}

	// read order from clients
	public static int readOrder(String message, User user) {
		
		String[] msgSplit;
		userActive(user);
		if (message == null) return 0;
		
		try {
			msgSplit = message.split(" ");
		} catch (Exception e) {
			return 0;//default return
		}
		if (msgSplit[0].equals("whoelse"))
			return 1;
		if (msgSplit[0].equals("wholasthr"))
			return 2;
		if (msgSplit[0].equals("broadcast"))
			return 3;
		if (msgSplit[0].equals("message"))
			return 4;
		if (msgSplit[0].equals("logout"))
			return 5;
		//default return
		return 0;
	}
	
	// thread process
	public static class SocketThread extends Thread {
		private Socket socket;
		private Calendar logIn;
		private BufferedReader in;
		private PrintWriter out;
		private User newUser;

		public SocketThread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			String threadUser = null;
			String threadPassword = null;
			int tryTimes = 0;
			
			System.out.println("A new client connected.");
			
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				return;
			}

			// check if it on blocklist
			for (int i = 0; i < blockUser.size(); i++) {
				User userTmp = blockUser.get(i);
				if (socket.getInetAddress().getHostAddress().equals(userTmp.socket.getInetAddress().getHostAddress())) {
					Long blockDif = Calendar.getInstance().getTimeInMillis() - userTmp.logIn.getTimeInMillis();
					if (blockDif < BLOCK_MINUTE * 60 * 1000) {
						out.println("sorry, you are now blocked for logging in with this IP");
						out.flush();
						System.out.println(socket.getInetAddress().getHostAddress() + " is rejected because of blocking");
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					} else {
						blockUser.remove(userTmp);
					}
				}
			}                                                 


			
			//log in
			while (true) {
				if (tryTimes == 3) {
					User blocked = new User(threadUser, Calendar.getInstance(), socket);
					blockUser.add(blocked);
					System.out.println(socket.getInetAddress().getHostAddress() + " is blocked.");
					out.println("Sorry, you have tried 3 times.You are blocked for " + BLOCK_MINUTE + " minutes");
					out.flush();
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}
				
				//log in checking
				try {
					out.println("Username:(Your have tried " + tryTimes + " times (3 times most))");
					out.flush();
					threadUser = in.readLine();
					out.println("Password:");
					out.flush();
					threadPassword = in.readLine();
					if(threadUser==null || threadPassword==null){
						throw new SocketException();
					}
				} catch (IOException e) {
					try {						
						socket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return;
				}
				
				//check if this user is already online
				try {
					for (int i = 0; i < onlineUser.size(); i++) {		
						if (onlineUser.get(i).userName.equals(threadUser)) {
							out.println("this user is already online, please reconnect to the server.");
							out.flush();
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					}catch (Exception e1) {
						e1.printStackTrace();
						return;
					}
				
				String realPassword = userRecord.get(threadUser);				
				//check username and password for applier
				if (!realPassword.equals(threadPassword)) {
					out.println("the username and password do not match");
					out.flush();
					tryTimes++;
					continue;
				} else {
					System.out.println(threadUser + " logged in");
					logIn = Calendar.getInstance();
					newUser = new User(threadUser, logIn, socket);
					onlineUser.add(newUser);
					out.println("welcome, " + threadUser+" !");
					out.flush();
					break;
					}
				
			}
			
			//running loop
			while (true) {
				String command;
				try {
					command = in.readLine();
					if(command==null){
					throw new SocketException();
					}
					System.out.println("Action : " + threadUser + ": " + command);
				} catch (IOException e) {
					try {
						socket.close();
					} catch (IOException ee) {
						ee.printStackTrace();
					}
					return;
				}
				
				//read command
				switch (readOrder(command,newUser)) {
				
				case 0:// invalid command
					break;
					
				case 1:// whoelse
					if (onlineUser.size() == 1) {
						out.println("No other users online now.");
						out.flush();
						break;
					}else{
						for (int i = 0; i < onlineUser.size(); i++) {
							if (!onlineUser.get(i).userName.equals(newUser.userName)) {
								User userTmp = onlineUser.get(i);
								out.println(userTmp.userName);
							}
						}
						out.flush();
					}
					break;
					
				case 2:// wholasthr
					Calendar currentTime = Calendar.getInstance();
					for (int i = 0; i < allUser.size(); i++) {
						User userTmp = allUser.get(i);
						if(userTmp.online){
							out.println(userTmp.userName);
							out.flush();
						}
						else{
							Long wholasthrDif = currentTime.getTimeInMillis() - userTmp.logIn.getTimeInMillis();
							if (wholasthrDif < LAST_HOUR * 60 * 60 * 1000) {
								out.println(userTmp.userName);
								out.flush();
								}
						}						
					}
					break;
					
					
				case 3:// broadcast 
					String brMessage = command.substring(10);
					int brNumber=0;
					for (int i = 0; i < onlineUser.size(); i++) {
						if (!onlineUser.get(i).userName.equals(newUser.userName)) {
							try {
								User userTmp = onlineUser.get(i);
								PrintWriter tmpOut = new PrintWriter(userTmp.socket.getOutputStream());
								tmpOut.println("[Broadcast] "+ newUser.userName+" : " +brMessage);
								tmpOut.flush();
								brNumber++;
								out.println("Broadcast message was sent to " + brNumber +" users");
								out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

					break;
								
				case 4://message
					String[] messageInfo = command.split(" ");
					String tmpUser,tmpMsg;
					
					if(messageInfo.length!=3){
						out.println("Please input information in correct format");
						out.flush();
						break;
					}
					tmpUser=messageInfo[1];
					tmpMsg=messageInfo[2];
					
					for (int i = 0; i < onlineUser.size(); i++) {
						if (onlineUser.get(i).userName.equals(tmpUser)) {
							User userTmp = onlineUser.get(i);
							PrintWriter tmpOut;
							try {
								tmpOut = new PrintWriter(userTmp.socket.getOutputStream());
								tmpOut.println("[Message] "+ newUser.userName +" : " + tmpMsg);
								tmpOut.flush();
								out.println("message was sent successfully!");
								out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					
					break;
					
				case 5:// logout;
					onlineUser.remove(newUser);
					try {
						PrintWriter tmpOut = new PrintWriter(newUser.socket.getOutputStream());
						tmpOut.println("byebye " + newUser.userName);
						tmpOut.flush();
						newUser.socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;

				}
			}
		}		
	}	
	
	//main function
	public static void main(String args[]) {
		int listenPort=4119;//default value as the homework example
		ServerSocket serversocket = null;
		
		// read port name
		if (args.length != 0)
			listenPort = Integer.parseInt(args[0]);
		
		// read user list
		try{
			readFile("user_pass.txt", userRecord);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		// start server socket		
		try {
			serversocket = new ServerSocket(listenPort);
			System.out.println("Sever running...");
		} catch (IOException e) {
			System.exit(0);
		}

		// start a new thread
		try {
			while (true) {
				new SocketThread(serversocket.accept()).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(!serversocket.isClosed()) serversocket.close();
			} catch (IOException e) {
				return;
			}
		}

	}
	
}
