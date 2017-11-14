package com.googlecode.garbagecan.test.socket.nio.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyClient {

	private final static Logger logger = Logger.getLogger(MyClient.class.getName());
	
	public static void main(String[] args) throws Exception {
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				SocketChannel socketChannel = null;
				try {
					socketChannel = SocketChannel.open();
					SocketAddress socketAddress = new InetSocketAddress("192.168.0.11", 1991);
					socketChannel.connect(socketAddress);

					sendData(socketChannel, "filename");
					String string ="";
					string = receiveData(socketChannel);
					if(!string.isEmpty()){
						socketChannel = SocketChannel.open();
						socketChannel.connect(new InetSocketAddress("192.168.0.11", 1991));
						sendData(socketChannel, string);
						receiveFile(socketChannel, new File("F:/"+string));
					}
				} catch (Exception ex) {
					logger.log(Level.SEVERE, null, ex);
				} finally {
					try {
						socketChannel.close();
					} catch(Exception ex) {}
				}
			}
			
		}).start();
	}
	

		private static String receiveData(SocketChannel socketChannel) throws IOException {
			String string = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			
			try {
				byte[] bytes;
				int size = 0;
				while ((size = socketChannel.read(buffer)) >= 0) {
					buffer.flip();
					bytes = new byte[size];
					buffer.get(bytes);
					baos.write(bytes);
					buffer.clear();
				}
				bytes = baos.toByteArray();
				string = new String(bytes);
			}catch(Exception ex){
				logger.log(Level.SEVERE, null, ex);
			}finally {
				try {
					baos.close();
				} catch(Exception ex) {
					
				}
			}
			return string;
		}

		private static void sendData(SocketChannel socketChannel, String string) throws IOException {
			byte[] bytes = string.getBytes();
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			socketChannel.write(buffer);
			socketChannel.socket().shutdownOutput();
		}
		
		private static void sendFile(SocketChannel socketChannel, File file) throws IOException {
			FileInputStream fis = null;
			FileChannel channel = null;
			try {
				fis = new FileInputStream(file);
				channel = fis.getChannel();
				ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
				int size = 0;
				while ((size = channel.read(buffer)) != -1) {
					buffer.rewind();
					buffer.limit(size);
					socketChannel.write(buffer);
					buffer.clear();
				}
				socketChannel.socket().shutdownOutput();
			} finally {
				try {
					channel.close();
				} catch(Exception ex) {}
				try {
					fis.close();
				} catch(Exception ex) {}
			}
		}

		private static void receiveFile(SocketChannel socketChannel, File file) throws IOException {
			FileOutputStream fos = null;
			FileChannel channel = null;
			
			try {
				fos = new FileOutputStream(file);
				channel = fos.getChannel();
				ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

				int size = 0;
				while ((size = socketChannel.read(buffer)) != -1) {
					buffer.flip();
					if (size > 0) {
						buffer.limit(size);
						channel.write(buffer);
						buffer.clear();
					}
				}
			} finally {
				try {
					channel.close();
				} catch(Exception ex) {}
				try {
					fos.close();
				} catch(Exception ex) {}
			}
		}
	}