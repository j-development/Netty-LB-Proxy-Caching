package org.ekdahl;

import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class JarStarter {

	@Getter
	public Process process;

	public JarStarter(int port) {
		final List<String> actualArgs = new ArrayList<String>();
		final Runtime re;
		actualArgs.add(0, "java");
		actualArgs.add(1, "-jar");
		actualArgs.add(2, "spring-app.jar");
		actualArgs.add(3, "--server.port=" + port);
		try {
			re = Runtime.getRuntime();
			process = re.exec(actualArgs.toArray(new String[0]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
