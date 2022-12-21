package org.ekdahl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;

import java.util.Scanner;

@Getter
public class ReverseProxyServer {
	private final NioEventLoopGroup bossGroup; // TODO: Instantiate here
	private final NioEventLoopGroup workerGroup;
	private final NodeOrchestrator nodeOrchestrator;
	private final String host;
	int port;


	public ReverseProxyServer() {
		this.port = 5000;
		this.host = "localhost";
		this.bossGroup = new NioEventLoopGroup(); // Event loop for boss threads
		this.workerGroup = new NioEventLoopGroup(); // Event loop for worker threads
		this.nodeOrchestrator = new NodeOrchestrator(this);
	}

	public void run() throws Exception {
		try {
			ServerBootstrap boot = new ServerBootstrap();
			boot
					.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new HttpReverseProxyInitializer(host, port, this));

			System.out.println("Starting connection on port: " + port);
			Channel ch = boot.bind(port).sync().channel();


			ch.closeFuture().sync();
		}
		finally {
			nodeOrchestrator.closeNodes();
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
