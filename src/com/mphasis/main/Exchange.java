package com.mphasis.main;

import jdk.jfr.internal.tool.Main;
import org.omg.SendingContext.RunTime;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class Exchange {
	public static Logger logger;
	static {
		logger=Logger.getLogger(Main.class.getName());
	}
	private boolean isStopped;
	
	//  1.  Listen for incoming connections
	//  2.  Start a thread for each connection  ( each thread will be a Connection object)
	private ServerSocket serverSock; //servers main listening socket.
	
//	private ConcurrentMap<String, Connection> clientFeeds;
	private ConcurrentMap<String, Connection> clientFeeds;
	private ConcurrentMap<Double, PriorityQueue<Order>> orderbook;
	
	public static void main(String args[]) throws IOException{
		
		//Read port from stdinput
		// Then init one Exchange object.  this will create all the message queues and order structs
		//  call runServer
		int port = Integer.parseInt(args[0]);
		Exchange OurExchange = new Exchange();
		
		OurExchange.runServer(port);
		
	}
	
	public Exchange(){
		// constructor for our main exchange
		orderbook = new ConcurrentHashMap<Double, PriorityQueue<Order>>();

		clientFeeds = new ConcurrentHashMap<String, Connection>();
		
		
	}
	public void runServer(int port) throws IOException{
		
		Socket clientSock = null;
		this.serverSock = new ServerSocket(port);
		
		
		
		while(!isStopped){
			try {
				
				clientSock = this.serverSock.accept();
			}// end try
			catch (IOException e) {

				logger.log(Level.INFO,"Error connecting to client");
			}
			
			
			int vCPU = Runtime.getRuntime().availableProcessors();
			ScheduledExecutorService service = Executors.newScheduledThreadPool(vCPU);

			service.schedule(new Connection(clientSock, this),1,TimeUnit.MILLISECONDS);
			
			
		}//end while 
		
		
	}// end runsever
	
	public void addOrder(Order orderToAdd)
	{

		logger.log(Level.INFO,"Exchange addOrder has been called");
		
		//if we cant fill right away, add the order to the orderbook
		if( !instantFill(orderToAdd))
		{
			//add the order to the book

			logger.log(Level.INFO,"Adding order to book now...");

			
			if( orderbook.containsKey(orderToAdd.price))
			{
				logger.log(Level.INFO,"Order book contains orders at that level, adding our new order..");

				orderbook.get(orderToAdd.price).add(orderToAdd);
				
				//Send a fill message to the correct client
				clientFeeds.get(orderToAdd.clientID).feedMessageQueue.add("Order added with ID: " + orderToAdd.orderID.toString());
				
			}
			else
			{

				logger.log(Level.INFO,"Order book has no orders at that price level, creating price level now...");
				PriorityQueue<Order> newprice = new PriorityQueue<Order>();
				newprice.add(orderToAdd);
				orderbook.putIfAbsent(orderToAdd.price, newprice );

				logger.log(Level.INFO,"Order has been added!");
				clientFeeds.get(orderToAdd.clientID).addMessage("Order has been added to the book, ID: "+ orderToAdd.orderID.toString());
				
				
			
			}
		
			
			
		}

	}
	
	public boolean instantFill(Order orderToFill){
		
		if (orderToFill.type == OrderType.BUY)
		{
			
			try 
			{
				for ( ConcurrentMap.Entry<Double, PriorityQueue<Order> > priceLevel : orderbook.entrySet())
				{
					for (Order individualOrder : priceLevel.getValue())
					{
						if (orderToFill.price <= individualOrder.price && individualOrder.type == OrderType.SELL)
						{
							priceLevel.getValue().remove(individualOrder);// remove the order.
							match(orderToFill, individualOrder);
							return true;		
						}			
					}
				
				}// end of outer for each
			}//end try
			
			
			catch (Exception e)
			{ System.out.println();
				logger.log(Level.INFO,"Exception in instant fill " + e.toString());
	}
		
			
		} // end order type IF statement
		else if (orderToFill.type == OrderType.SELL)
		{
	
			
			Order currentBestFill = new Order();
			for ( ConcurrentMap.Entry<Double, PriorityQueue<Order> > priceLevel : orderbook.entrySet())
			{
			
				//	for (Order individualOrder : priceLevel.getValue())
				//{
					Order individualOrder = priceLevel.getValue().peek();
					
					if (individualOrder == null)
						return false;
					if (orderToFill.price <= individualOrder.price && individualOrder.type == OrderType.BUY)
					{
						//potential match found,  lets keep looking for a better price.
					
						currentBestFill = individualOrder;	
						break;
					//	return true;		
					}			
				
			}// end oouter for
			
	
			if (currentBestFill.isRealOrder)
			{
				logger.log(Level.INFO,"Current Best Fill found in instant fill...");
				orderbook.get(currentBestFill.price).remove(currentBestFill);  // this may be buggy
				
				//priceLevel.getValue().remove(individualOrder);// remove the order.
				match(orderToFill, currentBestFill);
				return true;
			}
			
		} // end iff 

		logger.log(Level.INFO,"No Instant Fill Made, returning false..." );
		//
		return false;
		
	}
	
	public void match(Order orderOne, Order orderTwo)
	{
		// send fill notification to each clientID

		logger.log(Level.INFO,"Match made!");
		
		String fill = "Fill Notification!  Buy Side: " + orderOne.clientID + " Sell Side: " + orderTwo.clientID + " Price: " + String.valueOf(orderTwo.price) + "Quantity: " + String.valueOf(orderTwo.quantity) ;
		
		clientFeeds.get(orderOne.clientID).addMessage(fill);
		clientFeeds.get(orderTwo.clientID).addMessage(fill);
		
		
		//add to log somewhere*****
		
	}
	
	public void cancelOrder(String clientID, String orderID)
	{
		
		// iterate through the orderbook until you find the ORDER ID
		// when you do find it, remove it
		//then send message to the client.
		for ( ConcurrentMap.Entry<Double, PriorityQueue<Order> > priceLevel : orderbook.entrySet())
		{
		
				for (Order individualOrder : priceLevel.getValue())
				{
					logger.log(Level.INFO,"Comparing order id " + individualOrder.orderID.toString() + " to order id " + orderID);
					if( individualOrder.orderID.toString().equals(orderID) )
					{
						//remove that order!
						System.out.println();
						logger.log(Level.INFO,"Removing order # " + orderID);
						priceLevel.getValue().remove(individualOrder);
						
						
						
					}
						
				}
			
			
		}
		
		
	}
	
	public void sendMarketData(String clientID)
	{
		//giant string builder   print statement that is all (top) orders in the queue.

		logger.log(Level.INFO,"Sending Market Data...");

		StringBuilder book = new StringBuilder();
		
		
		// loop to build the book string
		for ( ConcurrentMap.Entry<Double, PriorityQueue<Order> > priceLevel : orderbook.entrySet()){
			
			Order order = priceLevel.getValue().peek();
			
			if (order != null)
			{
			book.append("Price: " + String.valueOf(order.price) + "    Quantity: " + String.valueOf(order.quantity) + " 	Type: " +  order.type.toString()  ) ;
			book.append(System.getProperty("line.separator"));
			}
			
		//	book.append(
			
			
			//book.append(str)
		}
		logger.log(Level.INFO, "Market Data assembled! " + book.toString());
		
		
		//send book to the client that requested it
			if(this.clientFeeds.containsKey(clientID))
			{
				logger.log(Level.INFO,"Client Feeds contains the key!");
				Connection ethan = this.clientFeeds.get(clientID);
				//System.out.println(ethan.toString());
				//System.out.println("book to string not working? " + book.toString());
				
				//ethan.feedMessageQueue.add(book.toString());
				ethan.addMessage(book.toString());
				
				//this.clientFeeds.get(clientID).feedMessageQueue.add(book.toString());
		
			}
			else
				logger.log(Level.INFO, "Could not find a FEEd connection to send market data too..");
			
	}
	
	
	public boolean registerClientFeed(String clientID, Connection connObject)
	{
		//this method needs to put clients in an exchange dictionary <clientID, connObject>
		logger.log(Level.INFO, "Putting clientID " + clientID + "Into client feeds object");
		this.clientFeeds.put(clientID,  connObject);

		return false;
		
	}
	public boolean removeClientFeed(String clientID )
	{
		this.clientFeeds.remove(clientID);
		return true;
		
	}
	
	
	//	
			//2b.  we will need to create a  dictionary of clientID, and the var representing
			// the thread so that messages can be sent to specific Ids i believe.
	// tthis dictionary will be public, and each Connection object will be responsible
	// for adding iteslf to the dictionary, as soon as it learns what 'type' of connection it 
	// is.  type being either exececution, or reporting .
	        
	//  3.  form threadsafe collection for incoming orders/messages

	// not sure we need the below...
	public ConcurrentLinkedQueue<Message> incomingQueue;

	
	//	4. write loop to continuously parse ^order queue 
	/*  logic for  loop is this
	 * 
	 * for each message in the messageQueue 
	 * 
	 * 		if its an order,  check the order book and see if it can be filled instantly
	 * 				if it can be filled, remove itself, and the the matched order and send
	 * 				a fill message to each clientID
	 * 
	 *      if it's an order cancelletion,  find the order and remove it.  send message to clientID
	 *      
	 */
	
	//  5.  Create data structure (or class) for an 'order book'
	//  this will need to store orders at various price levels and client ids 
	/*   The logic will be as follows
	 * 
	 * 
	 *   Generate a java map   Map <doubles, orders>
	 *   So it's a 2d data structure, the 'doubles' are the keys and represent 
	 *   order price levels, the 'orders' will be a separate class and will store the 
	 *   price (though it's self explanatory i guess), and the client ID that sent the order
	 *   We could add type to this, but probably not necessary right now.
	 * 
	 */
	
	//public ConcurrentMap<Double, Collection<String>> orderbook;  //order book for all prices.

}
