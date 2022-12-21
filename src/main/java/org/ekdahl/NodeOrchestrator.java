package org.ekdahl;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ekdahl.Utils.ProcessOutput;

public class NodeOrchestrator {

	private boolean alive = true;

	private int startingPort;
	private final List<Node> runningNodes, closingQueue, startingQueue;
	private final ReverseProxyServer server;
	//Minimum amount of nodes to keep running
	private int minNodes = 3;
	//Limit for when to start closing nodes, higher means slower deprovisioning of nodes
	private final int deprovisionLimit = 30; //30x2 seconds = 1 minute before deprovisioning
	private int deprovisionPoints;

	public NodeOrchestrator(ReverseProxyServer server) {
		this.runningNodes = new ArrayList<>();
		this.closingQueue = new ArrayList<>();
		this.startingQueue = new ArrayList<>();
		this.server = server;
		this.startingPort = 5071;
		this.provision(minNodes);
		this.deprovisionPoints = 0;
		new Thread(this::threadRunner).start();
	}

	public void provision(int amount) {
		System.out.println("Starting " + amount + " nodes");
		for (int i = 0; i < amount; i++) {
			Node node = new Node(startingPort);
			startingQueue.add(node);
			System.out.println("Started instance: " + node.getProcess().pid());
			startingPort++;
		}
	}

	// Random balancing
	public synchronized Node next() {
		if (runningNodes.isEmpty()) return null;
		int randomInt = (int) (Math.random() * runningNodes.size());
		return runningNodes.get(randomInt);
	}

	private void closeLast() {
		//TODO: Close node with least requests
		System.out.println("Last node is closing");
		// Switched back to closing last node, for all intent and purposes this is the same as closing the node with least requests
		int last = runningNodes.size() - 1;
		closingQueue.add(runningNodes.get(last));
		runningNodes.remove(last);
	}

	// Closing all nodes in each list
	public synchronized void closeNodes() {
		alive = false;
		runningNodes.forEach(Node::stop);
		closingQueue.forEach(Node::stop);
		startingQueue.forEach(Node::stop);

	}

	public synchronized void addStartedNode(Node node) throws IOException {
		ProcessOutput(node);
		startingQueue.remove(node);
		runningNodes.add(node);
	}


	//Could be extracted to a separate class "NodeManager" or something, for better readability
	private synchronized void nodeManage() {
		var iterator = startingQueue.iterator();
		while (iterator.hasNext()) {
			Node node = iterator.next();
			PingNode(node);
		}

		iterator = closingQueue.iterator();
		while (iterator.hasNext()) {
			var node = iterator.next();
			if (!node.getChannels().isEmpty()) continue;
			node.stop();
			iterator.remove();
		}

		double requests = requestsAVG();

		String nodePrintLine = "";

		for (int i = 0; i < runningNodes.size(); i++) {
			Node node = runningNodes.get(i);
			// Want to check if nodes are still alive
			synchronized (this) {
				checkNodeStatus(node);
			}
			if (node.getProcess() == null) continue;
			nodePrintLine += "pid:" + node.getProcess().pid() + " ";
		}


		if (!runningNodes.isEmpty()) {
			System.out.println(nodePrintLine + "| Requests: " + requests);
		}

		// If requests are above 1.0 avg, start a new Node
		if (requests > 1.0) {
			deprovisionPoints = 0;
			provision(1);
		}

		// If requests are below 0.5 avg it gets a point, starts closing a node when deprovision limit is reached
		if (requests < 0.5 && runningNodes.size() > minNodes ) {
			deprovisionPoints++;
			if (deprovisionPoints > deprovisionLimit) {
				deprovisionPoints = 0;
				closeLast();
			}
		}


	}

	private double requestsAVG() {
		var requests = 0.0;
		for (var node : runningNodes) {
			requests += node.getRequests();
			node.resetRequests();
		}

		requests = requests / (double) runningNodes.size();
		return requests;
	}

	private synchronized void checkNodeStatus(Node node) {
		//Checking if node is dead
		if (startingQueue.contains(node)) return;
		if (node.getProcess() != null && !node.getProcess().isAlive()) {
			try {
				System.out.println("Node is dead");
				node.stop();
				runningNodes.remove(node);
				provision(1); //Provision new node
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void PingNode(Node node) {
		// Health check if node is alive and running
		try {
			var bootstrap = new Bootstrap();
			bootstrap.group(server.getWorkerGroup())
					.channel(NioSocketChannel.class)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
					.handler(new ChannelInitializer<SocketChannel>() { // Inline anonymous class, could be extracted to a separate class
								 @Override
								 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

								 }

								 @Override
								 protected void initChannel(SocketChannel channel) throws Exception {
									 channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
										 @Override
										 protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {

										 }

										 @Override
										 public void channelActive(ChannelHandlerContext ctx) throws Exception {
											 addStartedNode(node);
											 ctx.close();
										 }
									 });
								 }
							 }

					)
					.connect(server.getHost(), node.getPort())
					.sync();
		} catch (Exception ignored) {
		}
	}

	// Lambda for the Thread
	private void threadRunner() {
		while (true) {
			synchronized (this) {
				if (!alive) {
					return;
				}
				nodeManage();
			}
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
