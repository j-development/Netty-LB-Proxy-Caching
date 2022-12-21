package org.ekdahl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class HttpReverseProxyInitializer extends ChannelInitializer<SocketChannel> {
	private final String host;
	private final ReverseProxyServer server;


	public HttpReverseProxyInitializer(String host, ReverseProxyServer server) {
		this.host = host;
		this.server = server;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel)  {
		socketChannel.pipeline()
				//.addLast(new LoggingHandler(LogLevel.INFO))   // Adding channelhandlers to the pipeline
				.addLast()
				.addLast(new HttpReverseProxyHandler(server, host));

	}
}
