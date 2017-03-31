/**
 * Router Class
 *
 * Router implements Dijkstra's algorithm for computing the minumum distance to all nodes in the network
 * @author      XYZ
 * @version     1.0
 *
 */

 import java.io.*;
 import java.net.*;
 import java.util.*;

public class Router {

 	/**
     	* Constructor to initialize the program
     	*
     	* @param peerip		IP address of other routers (we assume that all routers are running in the same machine)
     	* @param routerid	Router ID
     	* @param port		Router UDP port number
     	* @param configfile	Configuration file name
	    * @param neighborupdate	link state update interval - used to update router's link state vector to neighboring nodes
      * @param routeupdate 	Route update interval - used to update route information using Dijkstra's algorithm

  */

  // Timer
  private TimerTask timerTask;
  private Timer timer;

  // Socket
  private DatagramSocket socket;
  private DatagramPacket receivepacket;
  private DatagramPacket sendPacket;

  public final static int MAX_PAYLOAD_SIZE = Integer.SIZE/8 * 10; // bytes (i.e Max number of nodes is 10, integer size is 4 bytes in java)
  private int[] cost;
  private LinkState broadcast;

  private Config config;

  private String peerip;
  private int routerid;
  private int port;
  private String configfile;
  private int neighborupdate;
  private int routeupdate;

	public Router(String peerip, int routerid, int port, String configfile, int neighborupdate, int routeupdate) {

    this.peerip = peerip;
    this.routerid = routerid;
    this.port = port;
    this.configfile = configfile;
    this.neighborupdate = neighborupdate;
    this.routeupdate = routeupdate;

	}

    	/**
     	*  Compute route information based on Dijkstra's algorithm and print the same
     	*
     	*/
	public void compute() {

    //Initialization of data structure(s)
    timer = new Timer(true);

    config = new Config(configfile);

    cost = new int[config.nodes];
    for (int i = 0; i < config.nodes; i++) {
        cost[i] = 999;
    }

    try {

      //Create UDP socket to send and listen link state message
      socket = new DatagramSocket(port);

      //Set timer task to send node’s link state vector to neighboring nodes every 1000 ms
      timer.scheduleAtFixedRate(new SendLink(this), neighborupdate, neighborupdate);

      //Set timer task to update node’s route information every 10000 ms
      timer.scheduleAtFixedRate(new UpdateRoute(this), routeupdate, routeupdate);

      while(true) {

        //Receive link state message from neighbor
        byte[] message = new byte[MAX_PAYLOAD_SIZE];
        receivepacket = new DatagramPacket(message, message.length);
        socket.receive(receivepacket);

        // Update datastructures and forward link state messages
        processUpDateDS(receivepacket);
      }

    }
      catch (Exception e)
    {
      System.out.println("Error: " + e.getMessage());
    }
    finally
    {
      if (socket != null) { socket.close(); }
    }

	  	/**** You may use the follwing piece of code to print routing table info *******/
        	System.out.println("Routing Info");
        	System.out.println("RouterID \t Distance \t Prev RouterID");
      //  	for(int i = 0; i < numNodes; i++)
      //    	{
      //    		System.out.println(i + "\t\t   " + distancevector[i] +  "\t\t\t" +  prev[i]);
      //     	}

	 	/*******************/

	}

  public synchronized void processUpDateDS(DatagramPacket receivepacket)
  {
    System.out.println("Recieved Linkstate Message");

    //cost =

      // Update data structure(s).
      // Forward link state message received to neighboring nodes
      // based on broadcast algorithm used.
  }

  public synchronized void processUpdateNeighbor(){
    System.out.println("Update Neighbor");

    try{
      //Send node’s link state vector to neighboring nodes as link state message.
      for(int i = 0; i < config.neighbors; i++){
        broadcast = new LinkState(routerid, config.routers[i].ID, cost, config.nodes);
        sendPacket = new DatagramPacket(broadcast.getBytes(), broadcast.getBytes().length, InetAddress.getByName(peerip), config.routers[i].port);
        socket.send(sendPacket);
      }
    }
    catch (Exception e)
    {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public synchronized void processUpdateRoute(){
    System.out.println("Update Route");

    //If link state vectors of all nodes received,
    //Yes => Compute route info based on Dijkstra’s algorithm
    //and print as per the output format.
    //No => ignore the event.
    //Schedule task if Method-1 followed to implement recurring
    //timer task.
  }

	public static void main(String[] args) {

		String peerip = "127.0.0.1"; // all router programs running in the same machine for simplicity
		String configfile = "";
		int routerid = 999;
    int neighborupdate = 1000; // milli-seconds, update neighbor with link state vector every second
		int forwardtable = 10000; // milli-seconds, print route information every 10 seconds
		int port = -1; // router port number

		// check for command line arguments
		if (args.length == 3) {
			// either provide 3 parameters
			routerid = Integer.parseInt(args[0]);
			port = Integer.parseInt(args[1]);
			configfile = args[2];
		}
		else {
			System.out.println("wrong number of arguments, try again.");
			System.out.println("usage: java Router routerid routerport configfile");
			System.exit(0);
		}


		Router router = new Router(peerip, routerid, port, configfile, neighborupdate, forwardtable);

		System.out.printf("Router initialized..running");
		router.compute();
	}

}

class Config {

  public int nodes;
  public int neighbors;
  public Node[] routers;

  public Config (String config){

    try{
      String line;
      BufferedReader	read = new BufferedReader(new FileReader(config));

      nodes = Integer.parseInt(read.readLine());

      routers = new Node[nodes];

      neighbors = 0;

      System.out.println("config:");
	    while ((line = read.readLine()) != null) {
		      String[] configinfo = line.split(" ");
          routers[neighbors] = new Node(configinfo[0],Integer.parseInt(configinfo[1]),Integer.parseInt(configinfo[2]),Integer.parseInt(configinfo[3]));
          neighbors++;
          System.out.println(line);
	    }
      System.out.println("nodes " + nodes + "neighbors " + neighbors);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class Node {

  public String label;
  public int ID;
  public int cost;
  public int port;

  public Node(String label, int ID, int cost, int port) {
    this.label = label;
    this.ID = ID;
    this.cost = cost;
    this.port = port;
  }

}

class SendLink extends TimerTask {

  public Router node;
  public SendLink(Router router){
    node = router;
  }

  @Override
  public void run() {
    node.processUpdateNeighbor();
  }
}

class UpdateRoute extends TimerTask {

  public Router node;
  public UpdateRoute(Router router){
    node = router;
  }
  @Override
  public void run() {
    node.processUpdateRoute();
  }
}
