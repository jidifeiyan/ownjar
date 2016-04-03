package com.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ChatServer implements Runnable {
	// 选择器
	private Selector selector;
	// 注册ServerSocketChannel后的选择键
	private SelectionKey serverKey;
	// 标识是否运行
	private boolean isRun;
	// 当前聊天室中的用户名称列表
	private Vector<String> unames;
	
	//保存房间信息
	private Map<Integer,Set<SocketAddress>> roomMap = new HashMap<Integer,Set<SocketAddress>>();;
	// 时间格式化器
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 构造函数
	 * 
	 * @param port
	 *            服务端监控的端口号
	 */
	public ChatServer(int port) {
		isRun = true;
		unames = new Vector<String>();
		init(port);
	}

	/**
	 * 初始化选择器和服务器套接字
	 * 
	 * @param port
	 *            服务端监控的端口号
	 */
	private void init(int port) {
		try {
			// 获得选择器实例
			selector = Selector.open();
			// 获得服务器套接字实例
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			// 绑定端口号
			serverChannel.socket().bind(new InetSocketAddress(port));
			// 设置为非阻塞
			serverChannel.configureBlocking(false);
			// 将ServerSocketChannel注册到选择器，指定其行为为"等待接受连接"
			serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
			// 轮询选择器选择键
			while (isRun) {
				// 选择一组已准备进行IO操作的通道的key，等于1时表示有这样的key
				int n;
				try {
					n = selector.select();
					if (n > 0) {
						// 从选择器上获取已选择的key的集合并进行迭代
						Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							// 若此key的通道是等待接受新的套接字连接
							if (key.isAcceptable()) {
								// 记住一定要remove这个key，否则之后的新连接将被阻塞无法连接服务器
								iter.remove();
								// 获取key对应的通道
								ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
								// 接受新的连接返回和客户端对等的套接字通道
								SocketChannel channel = serverChannel.accept();
								if (channel == null) {
									continue;
								}
								// 设置为非阻塞
								channel.configureBlocking(false);
								// 将这个套接字通道注册到选择器，指定其行为为"读"
								channel.register(selector, SelectionKey.OP_READ);
							}
							// 若此key的通道的行为是"读"
							else if (key.isReadable()) {
								readMsg(key);
							} else if (key.isWritable()) {
								writeMsg(key);
							}
							
						}
					}
				} catch (IOException e) {
					
				}
			}
		
	}

	/**
	 * 从key对应的套接字通道上读数据
	 * 
	 * @param key
	 *            选择键
	 * @throws IOException
	 */
	private void readMsg(SelectionKey key) throws IOException {
		// 获取此key对应的套接字通道
		SocketChannel channel = null;
			channel = (SocketChannel) key.channel();
			// 创建一个大小为1024k的缓存区
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			int count = channel.read(buffer);
	}

	/**
	 * 写数据到key对应的套接字通道
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void writeMsg(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		// 这里必要要将key的附加数据设置为空，否则会有问题
		Object obj = key.attachment();
		key.interestOps(SelectionKey.OP_READ);
	}


	public static void main(String[] args) throws IOException, ParseException {

         ChatServer s = new ChatServer(9999);
         new Thread(s).start();
	}
}
