package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class App {
	public static void main(String[] args) throws Exception {
		final Logger log = LoggerFactory.getLogger(App.class);
		log.error("You should see this log statement!");

		if (args.length == 2 && args[0].equals("-o")) {
			String fileName = args[1];
			File file = new File(fileName).getAbsoluteFile();
			try (
				PrintStream os = new PrintStream(new FileOutputStream(file))
			) {
				log.info("Writing a file: " + file.getAbsolutePath());
				os.println("1");
			}
		} else {
			log.info("Not writing a file");
		}
	}
}