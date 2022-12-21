package org.ekdahl;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Node {

	private final int port;
	private Process process;
	@Setter
	private int requests;
	private List<Channel> channels;


	public Node(int port) {
		this.channels = new ArrayList<>();
		this.port = port;
		process = new JarStarter(port).getProcess();
		this.requests = 0;
	}

	public void stop() {
		if (process != null) {
			process.destroyForcibly();
			System.out.println("Stopped instance: " + process.pid());
			process = null;
		}
	}

	public synchronized void detachChannel(Channel channel) {
		channels.remove(channel);
	}

	public synchronized void attachChannel(Channel channel) throws Exception {
		this.channels.add(channel);
	}

	public synchronized void resetRequests() {
		this.requests = 0;
	}

	public synchronized void addRequest() {
		this.requests++;
	}


}
