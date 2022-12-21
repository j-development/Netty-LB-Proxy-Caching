package org.ekdahl;

import com.google.common.cache.LoadingCache;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;

public class HttpReverseProxyInitializer extends ChannelInitializer<SocketChannel> {
	private final String host;
	private final int port;
	private final ReverseProxyServer server;


	public HttpReverseProxyInitializer(String host, int port, ReverseProxyServer server) {
		this.host = host;
		this.port = port;
		this.server = server;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		socketChannel.pipeline()
				//.addLast(new LoggingHandler(LogLevel.INFO))   // Adding channelhandlers to the pipeline
				.addLast(new HttpReverseProxyHandler(server, port, host));

	}


}
