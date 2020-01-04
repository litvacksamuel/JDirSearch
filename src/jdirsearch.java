/**
@author Samuel Litvack
@description A simple tool to search for hidden files and directories on a website.
*/

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Calendar;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Type;
import org.xbill.DNS.TextParseException;

public class jdirsearch {
	public static JFrame ui;
	public static JTextField url_field, user_agent_field, wordlist_field, status_field, proxy_host_field, proxy_port_field;
	public static JLabel url_label, options_label, user_agent_label, wordlist_label, status_label, proxy_label;
	public static JButton btnscan, btnwordlist, btnstop;
	public static JTextArea results;
	public static JFileChooser open_wordlist;
	public static int i;
	public static File wordlist;
	public static URL url;
	public static int lines;
	public static JCheckBox use_proxy;
	public static HttpURLConnection req;
	public static Thread scan;

	public static void startUI(){
		ui = new JFrame();
		ui.setTitle("JDirSearch path/files discover");
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setLayout(null);
		ui.setSize(900, 500);
		ui.setLocationRelativeTo(null);
		ui.setResizable(false);

		url_label = new JLabel("URL: ");
		url_label.setBounds(50, 5, 100, 50);
		ui.add(url_label);

		url_field = new JTextField();
		url_field.setBounds(100, 20, 600, 20);
		url_field.setText("http://localhost");
		ui.add(url_field);

		btnscan = new JButton("Scan!");
		btnscan.setBounds(750, 15, 100, 25);

		btnstop = new JButton("Stop");
		btnstop.setBounds(750, 50, 100, 25);
		btnstop.setEnabled(false);
		ui.add(btnstop);

		options_label = new JLabel("Options");
		options_label.setBounds(450, 30, 100, 50);
		ui.add(options_label);

		user_agent_label = new JLabel("User-agent: ");
		user_agent_label.setBounds(50, 55, 100, 50);
		ui.add(user_agent_label);

		user_agent_field = new JTextField();
		user_agent_field.setBounds(150, 70, 500, 20);
		user_agent_field.setText("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
		ui.add(user_agent_field);

		proxy_label = new JLabel("Socks proxy: ");
		proxy_label.setBounds(50, 90, 100, 50);
		ui.add(proxy_label);

		proxy_host_field = new JTextField();
		proxy_host_field.setBounds(150, 105, 130, 20);
		proxy_host_field.setText("127.0.0.1");
		ui.add(proxy_host_field);

		proxy_port_field = new JTextField();
		proxy_port_field.setBounds(300, 105, 50, 20);
		proxy_port_field.setText("9150");
		ui.add(proxy_port_field);

		use_proxy = new JCheckBox("Enable", false);
		use_proxy.setBounds(360, 105, 100, 20);
		use_proxy.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e){
				if(e.getStateChange() == ItemEvent.SELECTED){
					proxy_host_field.setEnabled(false);
                    proxy_port_field.setEnabled(false);
                }else if(e.getStateChange() == ItemEvent.DESELECTED){
					proxy_host_field.setEnabled(true);
                    proxy_port_field.setEnabled(true);
                }
			}
		});
		ui.add(use_proxy);

		wordlist_label = new JLabel("Wordlist: ");
		wordlist_label.setBounds(50, 120, 100, 50);
		ui.add(wordlist_label);

		wordlist_field = new JTextField();
		wordlist_field.setBounds(150, 135, 200, 20);
		wordlist_field.setText("/tmp/wordlist.txt");
		ui.add(wordlist_field);

		btnwordlist = new JButton("Open");
		btnwordlist.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				open_wordlist = new JFileChooser();
				int owo = open_wordlist.showOpenDialog(null);
				if (owo == JFileChooser.APPROVE_OPTION) {
					Path path = open_wordlist.getSelectedFile().toPath();
					wordlist_field.setText(path.toString());
				}
			}
		});
		btnwordlist.setBounds(370, 130, 100, 25);
		ui.add(btnwordlist);

		results = new JTextArea(500, 500);
		results.setEditable(false);
		results.setLineWrap(true);
		results.setWrapStyleWord(true);
		results.setVisible(true);
		Color color = Color.BLUE;
		results.setSelectedTextColor(color);

		results.setFont(new java.awt.Font("Miriam Fixed", 0, 13));

		JScrollPane scroll = new JScrollPane(results, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 

		scroll.setBounds(50, 180, 800, 250);
		ui.add(scroll);

		btnscan.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				wordlist = new File(wordlist_field.getText());
				try{
					lines = countLinesOld(wordlist_field.getText());
				}catch(IOException ioe){
					JOptionPane.showMessageDialog(null, "Error.");
				}

				if(!url_field.getText().isEmpty() && !wordlist_field.getText().isEmpty() && wordlist.exists()){
					scan = new Thread(new Runnable(){
						@Override
						public void run(){
							results.setText(null);
							url_field.setEditable(false);
							wordlist_field.setEditable(false);
							btnwordlist.setEnabled(false);
							btnscan.setEnabled(false);
							btnstop.setEnabled(true);
					
							try (BufferedReader br = new BufferedReader(new FileReader(wordlist))) {
								String path;
								String code;
								results.append("Wordlist size: "+lines+"\n");
								while ((path = br.readLine()) != null) {

									try{
										url = new URL(url_field.getText()+"/"+path);
									}catch(MalformedURLException ex){
										JOptionPane.showMessageDialog(null, "Error.");
										break;
									}

									try{
										if(use_proxy.isSelected()){
											if(checkProxy(proxy_host_field.getText(), proxy_port_field.getText())){
												code = checkFileOrDir(url, path, true);
											}else{
												JOptionPane.showMessageDialog(null, "Proxy connection failed");
												break;
											}
										}else{
											code = checkFileOrDir(url, path, false);
										}
										status_field.setText(path);
										String timeStamp = new SimpleDateFormat("[HH-mm-ss]").format(Calendar.getInstance().getTime());
										if(code.equals("200")){
											results.append(timeStamp+" - Path: /"+path+" - "+code+" \n");
										}
									}catch(Exception ex){
										JOptionPane.showMessageDialog(null, "Error.");
										break;
									}
								}
							}catch(IOException fne){
								JOptionPane.showMessageDialog(null, "Error. Check the wordlist!");
							}
							url_field.setEditable(true);
							wordlist_field.setEditable(true);
							btnscan.setEnabled(true);
							btnstop.setEnabled(false);
							btnwordlist.setEnabled(true);
							status_field.setText(null);
						}
					});
					scan.start();
				}else{
					JOptionPane.showMessageDialog(null, "Error");
				}
			}
		});
		ui.add(btnscan);

		btnstop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				scan.stop();
				url_field.setEditable(true);
				wordlist_field.setEditable(true);
				btnscan.setEnabled(true);
				btnstop.setEnabled(false);
				btnwordlist.setEnabled(true);
				status_field.setText(null);
			}
		});

		status_label = new JLabel("Status: ");
		status_label.setBounds(50, 420, 100, 50);
		ui.add(status_label);

		status_field = new JTextField();
		status_field.setEditable(false);
		status_field.setBounds(120, 435, 150, 20);
		status_field.setForeground(Color.BLACK);
		ui.add(status_field);
		ui.setVisible(true);
	}

	public static String checkFileOrDir(URL url, String tofind, boolean use_proxy){
		String code = "";
		try{
			if(!use_proxy){
				req = (HttpURLConnection) url.openConnection();
				req.setInstanceFollowRedirects(false);
				req.setRequestProperty("User-Agent", user_agent_field.getText());
				req.setRequestProperty("Connection", "keep-alive");
				req.setRequestMethod("GET");
				code = Integer.toString(req.getResponseCode());
			}else{
				String proxy_host = proxy_host_field.getText();
				String proxy_port = proxy_port_field.getText();
				Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy_host, Integer.parseInt(proxy_port)));
			    req = (HttpURLConnection) url.openConnection(proxy);
			    req.setInstanceFollowRedirects(false);
				req.setRequestProperty("User-Agent", user_agent_field.getText());
				req.setRequestMethod("GET");
				code = Integer.toString(req.getResponseCode());
			}
		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Connection error.");
		}finally{
			req.disconnect();
		}
		return code;
	}

	public static boolean hasCloudflare(String url){
		boolean hasCloudflare = false;

		try{
			Record [] records = new Lookup(url, Type.NS).run();
			if(records != null){
				for(int i=0; i<records.length; i++){
					NSRecord ns = (NSRecord) records[i];
					if(ns.getTarget().toString().contains("cloudflare")){
						hasCloudflare = true;
					}else{
						hasCloudflare = false;
					}
				}
			}else{
				JOptionPane.showMessageDialog(null, "NS Error. (1)");
			}
		}catch(TextParseException ex){
			JOptionPane.showMessageDialog(null, "NS Error. (2)");
		}
		return hasCloudflare;
	}

	public static boolean hostIsUp(String host){
		boolean hostIsUp = false;
		try{
			req = (HttpURLConnection)new URL(host).openConnection();
			req.setInstanceFollowRedirects(false);
			req.connect();
			if(req.getResponseCode() == 200 || req.getResponseCode() == 302 || req.getResponseCode() == 404){
				hostIsUp = true;
			}
		}catch(Exception e) {
			hostIsUp = false;
		}finally{
			req.disconnect();
		}
		return hostIsUp;
	}

	public static boolean checkProxy(String host, String port){
		try{
			Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, Integer.parseInt(port)));
			req = (HttpURLConnection)new URL("https://www.google.com.ar").openConnection(proxy);
			req.setInstanceFollowRedirects(false);
			req.connect();
			return true;
		}catch(Exception e) {
			return false;
		}finally{
			req.disconnect();
		}
	}

	//Stackoverflow
	public static int countLinesOld(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}

	public static void main(String args[]){
		javax.swing.SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				startUI();
			}
		});
	}
}