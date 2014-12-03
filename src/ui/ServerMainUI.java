package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import beans.Smessage;


import messages.AbstractMessage;


import server.ClientMessages;
import server.Server;

public class ServerMainUI extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8199871373163526037L;

	private ClientMessages clientMsgGetter = ClientMessages.getInstance();
	
	private Server server;
	
	private JList<String> clientlist;
	private JButton pushBt;
	private DefaultListModel<String> clientlistModel;
	private DefaultListModel<String> msglistModel;
	private JPanel msgMangePanel;
	private JPanel msgPanel;
	private JTextField inputTopic;
	private JTextField inputPaylod;
	private JButton addMsgBt;
	private JList<String> cMsgList;
	
	public ServerMainUI() throws IOException{
		setTitle("LSN server");

		setExtendedState(1);

		setResizable(true);
		setDefaultCloseOperation(3);
		
		JPanel mainpanel = new JPanel();
		mainpanel.setLayout(new BorderLayout());
		
		clientlistModel=new DefaultListModel<String>();
		clientlist = new JList<String>(clientlistModel);
		clientlist.setAutoscrolls(true);
		JScrollPane scrollableList = new JScrollPane(clientlist);
		
		//TODO 服务器刚启动  没有客户端连接
//		List<String> clientIDs = server.getAllClientID();
//		System.out.println(clientIDs.toArray(new String[clientIDs.size()]));
		
		
		String[] tp = {"LSN7F000001","fdsfdsf","fdsfdsfsf","fdsfssfs"};
//		clientlist.setListData(tp);
		
		//listModel.addElement("LSN7F000001");
		mainpanel.add(clientlist,"Center");
		
		msgPanel = new JPanel();
		msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.X_AXIS));
		
		msgPanel.add(scrollableList);
		
		msglistModel = new DefaultListModel<String>();
		cMsgList = new JList<String>(msglistModel);
//		cMsgList.setListData(tp);
		cMsgList.setAutoscrolls(true);
		msgPanel.add(cMsgList);
		
		msgMangePanel = new JPanel();
		msgMangePanel.setLayout(new BoxLayout(msgMangePanel, BoxLayout.Y_AXIS));	
		
		inputTopic = new JTextField("topic");
		msgMangePanel.add(inputTopic);
		
		inputPaylod = new JTextField("payload");
		msgMangePanel.add(inputPaylod);
		
		addMsgBt = new JButton("add");
		addMsgBt.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(clientlist.getSelectedValue() != null)
					clientMsgGetter.addClientMessage(clientlist.getSelectedValue(), inputTopic.getText(), inputPaylod.getText());
			}
		});
		
		msgMangePanel.add(addMsgBt);
		
		msgPanel.add(msgMangePanel);
		
		mainpanel.add(msgPanel,"East");
		
		pushBt = new JButton("push");
		mainpanel.add(pushBt,"South");
		pushBt.setVisible(true);
		
		this.pushBt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				server.push(clientlist.getSelectedValue(),AbstractMessage.QOSType.LEAST_ONE,false);
			}
		});
		
		clientlist.addMouseListener(new MouseAdapter() {//获取选中用户消息  并添加到列表
			public void mouseClicked(MouseEvent e) {
				LinkedList<Smessage> msgs = clientMsgGetter.getClientAllMsg(clientlist.getSelectedValue());
				if(msgs != null){
					ArrayList<String> tplist = new ArrayList<String>();
					for(Smessage s:msgs){
						tplist.add(s.toString());
					}
					cMsgList.setListData(tplist.toArray(new String[0]));
				}

			}
		});
		
		this.setMinimumSize(new Dimension(600,400));
		this.centralWindow(this);
		add(mainpanel);
		initServer();
	}
	
	public void initServer() throws IOException{
//		ClientMessages clientMsgGetter = ClientMessages.getInstance();
//		clientMsgGetter.addClientMessage("LSN7F000001", "1", "1111");
//		clientMsgGetter.addClientMessage("LSN7F000001", "2", "2222");
//		clientMsgGetter.addClientMessage("LSN7F000001", "3", "3333");
		  
		server = new Server(){

			@Override
			protected void dealConnected(String clientID) {
				clientlistModel.addElement(clientID);
			}

			@Override
			protected void dealDisConnected(String clientID) {
				clientlistModel.removeElement(clientID);
			}
			
		};
		server.startServer();
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
