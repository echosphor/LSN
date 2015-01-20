package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
	
	private JTextField username; //用户名输入框
	private JPasswordField password; //密码输入框
	private JButton loginButton; //登录按钮
	private JLabel usernameLabel;
	private JLabel pwdLabel;
	private JPanel jp1,jp2,jp3;

	public ClientMainUI(){
		setTitle("LSN client");

		setExtendedState(1);

		setResizable(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		showLoginUI();
					
		this.setMinimumSize(new Dimension(300,300));
		centralWindow(this);
		initClient();
	}

	public void initClient(){
		client = new Client("localhost",Constants.PORT,"test","testwd",true){//TODO PUSH/POLL方式的切换

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
		//client.connect();
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
	
	public void showLoginUI(){
		this.setLayout(new GridLayout(3,1));
	
		jp1 = new JPanel();
		jp2 = new JPanel();
		jp3 = new JPanel();
		
		username = new JTextField(16);
		usernameLabel = new JLabel("用户名");
		password = new JPasswordField(16);
		pwdLabel = new JLabel("密码");
		loginButton = new JButton("登录");
		loginButton.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent evt){ 
				String pwd = new String(password.getPassword()); //得到密码
				String name = username.getText(); //得到密码
				client.setUserName(name);
				client.setPassWord(pwd);
				if(client.connect())
					showClientUI();
			}
		});
		
		jp1.add(usernameLabel);
		jp1.add(username);
		jp2.add(pwdLabel);
		jp2.add(password);
		jp3.add(loginButton);
		this.add(jp1);
		this.add(jp2);
		this.add(jp3);
		this.pack();
	}
	
	public void showClientUI(){		
		this.getContentPane().removeAll();
		this.repaint();
		this.validate();
		
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
		this.pack();
	}
}
