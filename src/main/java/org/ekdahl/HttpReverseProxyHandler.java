package org.ekdahl;

import com.google.common.cache.Cache;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpReverseProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private final ReverseProxyServer server;
	private final String host;
	private Channel channel;
	private final Cache<String, Object> cache;
	private String pathkey = "";

	public HttpReverseProxyHandler(ReverseProxyServer server, String host) {
		this.server = server;
		this.host = host;
		this.cache = CacheModule.getCache();
	}



	@Override // Called when a channel established a connection and the channel
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		var bootstrap = new Bootstrap();


		var clientChannel = ctx.channel();
		Node node = server.getNodeOrchestrator().next();
		if (node == null) {
			ctx.writeAndFlush("No nodes available");
			ctx.close();
		}
		System.out.println("Channel active");
//		ctx.writeAndFlush(new TextWebSocketFrame("Channel is active!"));
//		ctx.writeAndFlush("hey!"+InetAddress.getLocalHost().getHostName());
//		System.out.println("Host:" + InetAddress.getLocalHost().getHostName());
		if (node != null) {
			try {
				this.channel = bootstrap
						.group(server.getWorkerGroup())
						.channel(NioSocketChannel.class)
						.handler(new ChannelInitializer<SocketChannel>() {
							@Override
							public void initChannel(SocketChannel socketChannel) throws Exception {
								var pipeline = socketChannel.pipeline();
								pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
									@Override
									public void channelActive(ChannelHandlerContext ctx) throws Exception {
										node.attachChannel(ctx.channel());
									}

									@Override
									public void channelInactive(ChannelHandlerContext ctx) throws Exception {
										node.detachChannel(ctx.channel());
									}

									@Override
									protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
										node.addRequest();
										System.out.println("pid " + node.getProcess().pid());
										if(!Objects.equals(pathkey, "")){
											cache.put(pathkey, byteBuf.copy()); // Comment out this if you want to check out loadbalancing without caching
											pathkey = "";
										}

										clientChannel.writeAndFlush(byteBuf.copy());
									}

									@Override
									public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
										ctx.channel().close();
										clientChannel.close();
									}
								});
							}
						})
						.connect(host, node.getPort())
						.channel();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override // Called when then channel is closed
	public void channelInactive(ChannelHandlerContext ctx) {
		System.out.println("Channel inactive");
		if (this.channel != null) {
			this.channel.close();
		}
	}

	// This method is called when a message is received, in this case a ByteBuf, could also be a FullHttpRequest
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
		var buf1 = buf.copy();
		String key = keyFromRequest(buf1);
		cacheAwareHandler(ctx, buf, key);
	}

	private void cacheAwareHandler(ChannelHandlerContext ctx, ByteBuf buf, String key) {
		Object data = cache.getIfPresent(key);
		ByteBuf buf1 = (ByteBuf) data;
		if (data != null) {
			buf1.retain();
			ctx.writeAndFlush(buf1);
		} else {
			pathkey = key; // Setting global pathkey
			channel.writeAndFlush(buf.copy());
		}
	}

	private static String keyFromRequest(ByteBuf buf1) {
		String path = buf1.toString(Charset.forName("UTF-8"));
		var split = path.split(" ");
		var key = Arrays.stream(split).limit(2).collect(Collectors.joining(""));
		return key;
	}

	@Override // Called when an exception is caught
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.channel().close();
		channel.close();
	}
}
