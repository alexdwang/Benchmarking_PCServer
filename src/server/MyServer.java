package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ObjectOutputStream;

import weka.core.Instances;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.functions.Logistic;
import weka.classifiers.Classifier;


public class MyServer {
	private final static Logger logger = Logger.getLogger(MyServer.class.getName());
	private final static String projectHome = "F:/Benchmarking_Server/";
	private final static String traindataFileName = "traindata";
	private final static String modelFileName = "model";
	static Classifier[] cls;
	static String lastExecutionTime = "";
	
	public static void main(String[] args) {
		Selector selector = null;
		ServerSocketChannel serverSocketChannel = null;
		
		try {
			selector = Selector.open();
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().setReuseAddress(true);
			serverSocketChannel.socket().bind(new InetSocketAddress(1991));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			while (selector.select() > 0) {
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey readyKey = it.next();
					it.remove();

					SocketChannel socketChannel = null;
					String message = "";
					try {
						socketChannel = ((ServerSocketChannel) readyKey.channel()).accept();
						message = receiveOneData(socketChannel);
						System.out.println("Meaasge received: "+ message);
						logger.log(Level.INFO, message);
						if (message.equals("sending")){
							// receive training data from mobile device
							sendData(socketChannel, "receiving");
							receiveFile(socketChannel, new File(projectHome + traindataFileName));
						} else if(message.contains("|")||message.equals("nb")||message.equals("dt")||message.equals("lr")){
							trainModels(message, socketChannel);
						} else if(message.equals("model")){
							// send model file to mobile device
							File f= new File(projectHome + modelFileName);
							sendFile(socketChannel,f);
						} else if(message.equals("execution_time")){
							// send execution time to mobile device
							sendData(socketChannel, lastExecutionTime);
						}
					}catch(Exception ex){
						logger.log(Level.SEVERE, "1", ex);
					} finally {
						try {
							socketChannel.close();
						} catch(Exception ex) {
							logger.log(Level.SEVERE, "2", ex);
						}
					}
				}
			}
		} catch (ClosedChannelException ex) {
			logger.log(Level.SEVERE, "3", ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "4", ex);
		} finally {
			try {
				selector.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "5", ex);
			}
			try {
				serverSocketChannel.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "6", ex);
			}
		}
	}
	
	private static void trainModels(String message, SocketChannel socketChannel){
		try {
			StringBuffer sb = new StringBuffer();
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(projectHome + traindataFileName));
	
			Instances TrainDes = (Instances) ois.readObject();
			ois.close();
			String choices[] = message.split("\\|");
			cls = new Classifier[choices.length];
			for (int i = 0; i < choices.length; i++){
				Classifier mycls;
				long startTime=System.currentTimeMillis();
				if (choices[i].equals("nb")){
					mycls = new NaiveBayes(); // NB classifier
				} else if (choices[i].equals("dt")){
					mycls = new J48();
				} else /*if (choices[i].equals("lr"))*/{
					mycls = new Logistic();
				}
				mycls.buildClassifier(TrainDes);
				cls[i] = mycls;
				long endTime=System.currentTimeMillis();
				sb.append(String.valueOf(endTime - startTime)).append("ms|");
			}
			lastExecutionTime = sb.deleteCharAt(sb.length() - 1).toString();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(projectHome + modelFileName));
			oos.writeObject(cls);
			oos.flush();
			oos.close();
			
			File f= new File(projectHome + modelFileName);
		    if (f.exists() && f.isFile()){  
		        sendData(socketChannel, "model");
		    }else{  
		        logger.info("file doesn't exist or is not a file");  
		    }
		} catch (Exception e){
			System.out.println("Error while training models");
			e.printStackTrace();
		}
	}
	
	private static String receiveOneData(SocketChannel socketChannel) throws IOException {
		String string = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
		try {
			byte[] bytes;
			int size = 0;
			
			size = socketChannel.read(buffer);
			System.out.println(size);
			buffer.flip();
			bytes = new byte[size];
			buffer.get(bytes);
			baos.write(bytes);
			buffer.clear();
			
			bytes = baos.toByteArray();
			string = new String(bytes);
		}catch(Exception ex){
			logger.log(Level.SEVERE, "7", ex);
		}finally {
			try {
				baos.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "8", ex);
			}
		}
		return string;
	}
	
	private static String receiveData(SocketChannel socketChannel) throws IOException {
		String string = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
		try {
			byte[] bytes;
			int size = 0;
			
			while ((size = socketChannel.read(buffer)) > 0) {
				System.out.println(size);
				buffer.flip();
				bytes = new byte[size];
				buffer.get(bytes);
				baos.write(bytes);
				buffer.clear();
			}
			bytes = baos.toByteArray();
			string = new String(bytes);
		}catch(Exception ex){
			logger.log(Level.SEVERE, "7", ex);
		}finally {
			try {
				baos.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "8", ex);
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
		}catch(Exception ex){
			logger.log(Level.SEVERE, "9", ex);
		} finally {
			try {
				channel.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "10", ex);
			}
			try {
				fos.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "11", ex);
			}
		}
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
				System.out.println(buffer);
				socketChannel.write(buffer);
				buffer.clear();
			}
			socketChannel.socket().shutdownOutput();
		}catch(Exception ex){
			logger.log(Level.SEVERE, "12", ex);
		} finally {
			try {
				channel.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "13", ex);
			}
			try {
				fis.close();
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "14", ex);
			}
		}
	}
}
