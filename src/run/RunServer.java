package run;

import java.io.IOException;
import ui.ServerMainUI;

public class RunServer {

	public static void main(String[] args) throws IOException {
		ServerMainUI frame = new ServerMainUI();
		frame.setDefaultCloseOperation(3);
		frame.setVisible(true);
	}
	
}
