package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.block.Block;

public class Rollback implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();

	Rollback(Connection conn, String name, int minutes, World world, String table)
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		edits.clear();
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` WHERE player = ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDERr BY date DESC", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, minutes);
			rs = ps.executeQuery();

			while (rs.next()) {
				Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), world);
				edits.offer(e);
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
			}
		}
		
	}
	
	public int count()
	{
		return edits.size();
	}
	
	public void run()
	{
		Edit e = edits.poll();

		while (e != null)
		{
			e.perform();
			e.log();
			e = edits.poll();
		}
	}
	
	private class Edit
	{
		int type, replaced;
		int x, y, z;
		byte data;
		World world;
		
		Edit(int type, int replaced, byte data, int x, int y, int z, World world)
		{
			this.type = type;
			this.replaced = replaced;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void perform()
		{
			Block block = world.getBlockAt(x, y, z);
			if (block.getTypeId() == type || (block.getTypeId() >= 8 && block.getTypeId() <= 11))
			{
				if (block.setTypeId(replaced)) {
					block.setData(data);
					log.info("R (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
				}
				else
					log.info("r (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
			}
		}
		
		public void log()
		{
			int current = world.getBlockTypeIdAt(x, y, z);
			if (current == type)
				log.info("+ (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
			else
				log.info("- (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
		}
	}
}