package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import commons.Constants;

import client.Client;

public class ClientMainUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1112478004856971565L;
	
	private JTextArea text;
//	private JScrollPane scroll;
	private JButton pollBt;
	private Client client;
	private JLabel label;
	private boolean isPush;

	public ClientMainUI(){
		setTitle("LSN client");

		setExtendedState(1);

		setResizable(true);
		setDefaultCloseOperation(3);
		this.setLayout(new BorderLayout());
		label = new JLabel("PUSH");
		isPush=true;
		
		this.add(label,"North");
		
		text = new JTextArea();
		text.setEditable(false);
		text.setLineWrap(true);  
		
//		scroll = new JScrollPane(text);  
//		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
		
		this.add(text,"Center");
//		this.add(scroll,"East");
		
		pollBt = new JButton("POLL/PUSH");
		pollBt.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				client.switchMode();
				if(isPush)
					label.setText("POLL");
				else
					label.setText("PUSH");
				isPush=!isPush;
			}
			
		});
		this.add(pollBt,"South");
		
		this.setMinimumSize(new Dimension(300,300));
		centralWindow(this);
		initClient();
	}

	public void initClient(){
		client = new Client("localhost",Constants.PORT,"test","test",true){//TODO PUSH/POLL方式的切换

			@Override
			protected void dealPushed(String topicName, byte[] payload) {
				String msg = new String();;
				try {
					msg = "topic:"+topicName+"\n"+"payload:"+new String(payload, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				text.setText(text.getText()+"\n"+msg);
			}

			@Override
			protected void dealPollResp(String topicName, byte[] payload) {
				String msg = new String();;
				try {
					msg = "topic:"+topicName+"\n"+"payload:"+new String(payload, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				text.setText(text.getText()+"\n"+msg);
			}

			
		};
		client.connect();
	}
	
	public void centralWindow(Window w){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize =w.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        w.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);

	}
}
