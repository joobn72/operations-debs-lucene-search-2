/*
 * Created on Jan 16, 2007
 *
 */
package org.wikimedia.lsearch.frontend;

/**
 * RPC frontend to {@link IndexDaemon} using Apache XMLRPC package.
 * 
 * @author Brion Vibber
 *
 */
public class RPCIndexDaemon {
	private static IndexDaemon ud = null;

	public RPCIndexDaemon() {
		if(ud == null)
			ud = new IndexDaemon();
	}

	public String getStatus() {
		return ud.getStatus();
	}

	public boolean stop() {
		ud.stop();
		return true;
	}

	public boolean start() {
		ud.start();
		return true;
	}

	public boolean flushAll() {
		ud.flushAll();
		return true;
	}

	public boolean quit() {
		ud.quit();
		return true;
	}

}
