/**
 * Router Class
 *
 * Router implements Dijkstra's algorithm for computing the minumum distance to all nodes in the network
 * @author      Matthew hylton (10114326)
 * @version     2.0
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
  private LinkState receive;

  // Router Configuration
  private Config config;

  // List of linkstate messages by router
  private int[][] linkstates;

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

    // Initialization of data structure(s).
    timer = new Timer(true);

    config = new Config(configfile);

    cost = new int[config.nodes];
    for (int i = 0; i < config.nodes; i++) { cost[i] = 999; }
    cost[routerid] = 0;
    for (int i = 0; i < config.neighbors; i++) {
      cost[config.routers[i].ID] = config.routers[i].cost;
    }

    linkstates = new int[config.nodes][];

    try {

      // Create UDP socket to send and listen link state message.
      socket = new DatagramSocket(port);

      // Set timer task to send node’s link state vector to neighboring nodes every 1000 ms.
      timer.scheduleAtFixedRate(new SendLink(this), neighborupdate, neighborupdate);

      // Set timer task to update node’s route information every 10000 ms.
      timer.scheduleAtFixedRate(new UpdateRoute(this), routeupdate, routeupdate);

      while(true) {

        // Receive link state message from neighbor.
        byte[] message = new byte[1024];
        receivepacket = new DatagramPacket(message, message.length);
        socket.receive(receivepacket);

        // Update datastructures and forward link state messages.
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

	}

  public synchronized void processUpDateDS(DatagramPacket receivepacket){
      try{
        receive = new LinkState(receivepacket);

        // Update data structure.
        linkstates[receive.sourceId] = receive.cost;

        // Forward link state message received to neighboring nodes.
        if(receive.counter > 0){
          for(int i = 0; i < config.neighbors; i++){
            receive.counter--;
            sendPacket = new DatagramPacket(receive.getBytes(), receive.getBytes().length, InetAddress.getByName(peerip), config.routers[i].port);
            socket.send(sendPacket);
          }
        }
      }
      catch (Exception e)
      {
        System.out.println("Error: " + e.getMessage());
      }
  }

  public synchronized void processUpdateNeighbor(){
    try{
      // Send node’s link state vector to neighboring nodes as link state message.
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

    boolean allReceived = true;

    int dist[] = new int[config.nodes]; // D(v)
    int prev[] = new int[config.nodes];

    // Determine if link state vectors have been received.
    for (int i = 0; i < config.nodes; i++) {
      if (linkstates[i] == null){ allReceived = false; }
    }

    // If link state vectors of all nodes received.
    if (allReceived){

      // Compute route info based on Dijkstra’s algorithm
      int n[] = new int[config.nodes]; // N'
      int min = 999;
      int w = 0;

      for (int i = 0; i < config.nodes; i++) { n[i] = -1; }
      for (int v = 0; v < config.nodes; v++) { dist[v] = 999; }

      // N' = {u}. u = routerid.
      n[routerid] = routerid;

      // For all nodes v. If v is adjacent to u, then D(v) = c(u,v), else D(v = 999).
      for (int i = 0; i < config.neighbors; i++) {
        dist[config.routers[i].ID] = linkstates[routerid][config.routers[i].ID];
        prev[config.routers[i].ID] = routerid;
      }
      dist[routerid] = 0;
      prev[routerid] = routerid;

      // Loop until all nodes in N'.
      for (int j = 0; j < config.nodes-1; j++) {
        // Find node w not in N' such that D(w) is a minumum.
        for (int i = 0; i < config.nodes; i++) {
          if (n[i] == -1) {
            if (dist[i] < min) {
              min = dist[i];
              w = i;
            }
          }
        }

        // Add w to N'.
        n[w] = w;

        // Update D(v) for all adjacent to w and not in N'.
        for (int v = 0; v < config.nodes; v++) {
          if (n[v] == -1) {
            if (linkstates[w][v] < 999){
              // D(v) = min( D(v), D(w) + c(w,v)).
              if (dist[v] > dist[w]+linkstates[w][v])
                dist[v] = dist[w]+linkstates[w][v];
                prev[v] = w;
            }
          }
        }
      }

      //and print as per the output format.
      System.out.println("\nRouting Info");
      System.out.println("RouterID \t Distance \t Prev RouterID");
      for(int i = 0; i < config.nodes; i++)
      {
        System.out.println(i + "\t\t   " + dist[i] +  "\t\t\t" +  prev[i]);
      }
    }
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

// Network Configuration File Parser
class Config {

  public int nodes; // Number of nodes in the network
  public int neighbors; // Number of neigbors to this node
  public Node[] routers; // List of routers on the network

  public Config (String config){

    try{
      // Read The file
      String line;
      BufferedReader	read = new BufferedReader(new FileReader(config));

      // Parse Data from file
      nodes = Integer.parseInt(read.readLine());

      routers = new Node[nodes];

      neighbors = 0;

	    while ((line = read.readLine()) != null) {
		      String[] configinfo = line.split(" ");
          routers[neighbors] = new Node(configinfo[0],Integer.parseInt(configinfo[1]),Integer.parseInt(configinfo[2]),Integer.parseInt(configinfo[3]));
          neighbors++;
	    }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

// Routers in a Configuration file
class Node {

  public String label; // Router name
  public int ID; // Router id
  public int cost; // Cost to this router
  public int port; // Router port

  public Node(String label, int ID, int cost, int port) {
    this.label = label;
    this.ID = ID;
    this.cost = cost;
    this.port = port;
  }

}

// *** Timers *** //

// Send linkstate message timer
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

// Update shortest route timer
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
