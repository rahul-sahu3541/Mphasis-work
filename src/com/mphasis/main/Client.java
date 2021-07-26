package com.mphasis.main;

import jdk.jfr.internal.tool.Main;

import java.net.Socket;
import java.util.Queue;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {


	// This class represents the initial LOCAL running client.
	
  // methods
	/*
	 *  1.  connect to server and passes  clientID, type EXEC 
	 *  
	 *  	1a.  make a NEW ClientGateway object passing client ID
	 *  	 the Client gateway reader is a SECOND SOCKET, and it
	 *         listens for messages from gateway.
	 *  
	 *  2.  send orders to server -> wait for stdin, or show menu etc.
	 *  
	 *  
	 *  
	 *  3. send order cancelleations
	 *  
	 *  4. request market data.
	 * 
	 * 
	 * 
	 */
	//private String clientID;
	//private ClientConnectionType connectionType;
	
	
	
	//Args from client  args[clientID, connectionType]
	public static void main(String args[]) throws IOException
	{
		Logger logger;
		logger = Logger.getLogger(Main.class.getName());
		String clientID = args[0];
		String connectionType = args[1];
		int port = Integer.parseInt(args[2]);
		
		
		switch(connectionType){
		
		case "FEED":
			logger.log(Level.INFO, "Starting feed method call...");
		startFeed(clientID, port);	
		//	new Thread(	new ClientGatewayReader()).start();					
		
				
						
			break;
			
		case "EXEC":
			//create exec connection
			startExec(clientID, port);
			
			//new Thread(	new ClientGatewayExec()).start();	
			
			
			break;
			
		default:
			logger.log(Level.INFO, "Could not read connection type. Exiting...");

			
		}
		
		
		
	}
	
	public static void startExec(String clientID, int port) throws IOException{
		Logger logger;
		logger = Logger.getLogger(Main.class.getName());
		//read args from user input.
		//open socket connection to server.
		Socket connExchange = new Socket("localhost", port);
		BufferedReader input = new BufferedReader(new InputStreamReader(connExchange.getInputStream()));
		PrintWriter output = new PrintWriter(connExchange.getOutputStream(), true);
		
		
		// send initial message to server to identify connection type.  array[clientID, Type]
		String initExec = clientID + "|" +  "EXEC";

		logger.log(Level.INFO, "Sending client Id and exec feed to exchange...");
		output.println(initExec);
		
		boolean isRunning = true;
		//Loop asking for user input, sending orders etc.    
		
		
		// format for exchange exec messages
		// New orders =  clientID + '|' + messageType + '|' + [orderType] + '|' + [quantity]+ '|' + [price]
		// Cancel Orders = clientID + '|' + messageType + '|' + [orderID]
		// request for market data = clientID + '|' + messageType
		Console c = System.console();
		while(isRunning)
		{
			printMenu();
			
			String command = c.readLine();
			int action = Integer.parseInt(command);
			switch(action){
			case 1:
				//create order

				logger.log(Level.INFO,"Order Entry:  [BUY/SELL] | [quantity] | [price]");
				String order = c.readLine();
				String[] orderArray = order.split("\\|");

				logger.log(Level.INFO,"Making Order, field1: " + orderArray[0]);
				logger.log(Level.INFO,"Making Order, field2: " + orderArray[1]);
				logger.log(Level.INFO,"Making Order, field3: " + orderArray[2]);
				
				

				logger.log(Level.INFO,"Sending order...");

				output.println(clientID + "|" + "NewOrder" + "|" + orderArray[0] + "|" + orderArray[1] + "|" + orderArray[2]);
				
				
				break;
				
			case 2: 
				//cancel order
				String cancelID = c.readLine("Enter Order ID to Cancel");


				
				output.println(clientID + "|" + "CancelOrder" + "|" + cancelID );
				
				break;
				
			case 3:
				//request market data

				logger.log(Level.INFO,"Requesting market data...");

				output.println(clientID + "|" + "MarketData");
				
				
				break;
			case 4:
				//exit
				isRunning = false;
				break;
				
			default:

				logger.log(Level.INFO,"Not a valid option");
				break;
			
			}

			logger.log(Level.INFO,"Client exiting...");

		}

	}
	public static void printMenu(){
		Logger logger;
		logger = Logger.getLogger(Main.class.getName());;
		logger.log(Level.INFO,"You are connected to the exchange as an EXEC Feed:");
		logger.log(Level.INFO,"1 : Create Order" );
		logger.log(Level.INFO,"2 : Cancel Order");
		logger.log(Level.INFO,"3 : Request Market Data");
		logger.log(Level.INFO,"4 : Quit");

		
		
	}
	
	public static void startFeed(String clientID, int port) throws IOException{
		Logger logger;
		logger=Logger.getLogger(Main.class.getName());
		Socket connFeedExchange = new Socket("localhost", port);
		BufferedReader input = new BufferedReader(new InputStreamReader(connFeedExchange.getInputStream()));
		PrintWriter output = new PrintWriter(connFeedExchange.getOutputStream(), true);
		
		
		// send initial message to server to identify connection type.  array[clientID, Type]
		String initExec = clientID + "|" +  "FEED";
		logger.log(Level.INFO,"Sending ID and feed request to server " + clientID );
		output.println(initExec);
		
		boolean isRunning = true;
		
		while(isRunning)
		{
			logger.log(Level.INFO,"Feed is ready! Waiting for lines from exchange....");

			String feed = input.readLine();

			System.out.println("Message from gateway received!");
			System.out.println(feed);
			logger.log(Level.INFO,"Message from gateway received!");
			logger.log(Level.INFO,feed.toString());
		}
		
	}
	
	
}


