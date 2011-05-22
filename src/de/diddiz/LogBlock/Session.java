package de.diddiz.LogBlock;

public class Session
{
	public QueryParams lastQuery = null;
	public QueryParams toolQuery;
	public QueryParams toolBlockQuery;
	public boolean toolEnabled = true;
	public boolean toolBlockEnabled = true;

	Session(LogBlock logblock) {
		toolQuery = logblock.getConfig().toolQuery.clone();
		toolBlockQuery = logblock.getConfig().toolBlockQuery.clone();
	}
}
