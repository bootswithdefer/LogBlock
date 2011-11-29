package de.diddiz.LogBlock;

public enum Logging {
	BLOCKPLACE(true), BLOCKBREAK(true), SIGNTEXT, TNTEXPLOSION(true), CREEPEREXPLOSION(true), GHASTFIREBALLEXPLOSION(true), MISCEXPLOSION, FIRE(true), LEAVESDECAY, LAVAFLOW, WATERFLOW, CHESTACCESS, KILL, CHAT, SNOWFORM, SNOWFADE, DOORINTERACT, SWITCHINTERACT, CAKEEAT, ENDERMEN;

	public static int length = Logging.values().length;

	private final boolean defaultEnabled;

	private Logging() {
		this(false);
	}

	private Logging(boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}

	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}
}
