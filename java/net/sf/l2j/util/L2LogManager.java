package net.sf.l2j.util;

import java.util.logging.LogManager;

/**
 * Dummy class to enable logs while shutting down
 *
 */
public class L2LogManager extends LogManager {

	public L2LogManager() {
		super();
	}

	public void reset() {
		// do nothing
	}

	public void doReset() {
		super.reset();
	}
}