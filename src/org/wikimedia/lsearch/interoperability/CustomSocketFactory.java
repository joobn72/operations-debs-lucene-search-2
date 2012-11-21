package org.wikimedia.lsearch.interoperability;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import org.wikimedia.lsearch.config.GlobalConfiguration;

public class CustomSocketFactory
	implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable 
{
	static CustomSocketFactory instance;

	public CustomSocketFactory() {
	}

	public static CustomSocketFactory getInstance() {
		if (instance == null) {
			instance = new CustomSocketFactory();
		}
		return instance;
	}

	protected int getConnectTimeout() {
		return GlobalConfiguration.getInstance().getRMIConnectTimeout();
	}

	protected int getReadTimeout() {
		return GlobalConfiguration.getInstance().getRMIReadTimeout();
	}

	public Socket createSocket(String host, int port) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress(host, port), getConnectTimeout());
		s.setSoTimeout(getReadTimeout());
		return s;
	}

	protected class CustomServerSocket extends ServerSocket {
		public CustomServerSocket(int port) throws IOException {
			super(port);
		}

		public Socket accept() throws IOException {
			Socket s = super.accept();
			s.setSoTimeout(getReadTimeout());
			return s;
		}
	}

	public ServerSocket createServerSocket(int port) throws IOException {
		return new CustomServerSocket(port);
	}

	/** The Java API docs specify that equals() should be overridden to
	 * return true when socket factory instances are "functionally equivalent".
	 */
	public boolean equals(Object obj) {
		return obj.getClass().equals(getClass());
	}

	public int hashCode() {
		return getClass().hashCode();
	}
}
