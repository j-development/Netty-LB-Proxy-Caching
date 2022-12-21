package org.ekdahl;

import java.io.IOException;

public class Utils {

	public static void ProcessOutput(Node node) throws IOException {
		var input = node.getProcess().getInputStream();
		var bytes = new byte[10000];
		var read = input.read(bytes);
		System.out.println(new String(bytes, 0, read));
	}
}
