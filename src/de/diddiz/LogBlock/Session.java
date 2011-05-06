package de.diddiz.LogBlock;

public class Session {
	private QueryParams last = null;

	public QueryParams getLastQuery() {
		return last;
	}

	public void setLast(QueryParams params) {
		last = params;
	}
}
