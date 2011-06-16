package de.diddiz.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public class Utils
{
	public static void download(Logger log, URL url, File file) throws IOException {
		if (!file.getParentFile().exists())
			file.getParentFile().mkdir();
		if (file.exists())
			file.delete();
		file.createNewFile();
		final int size = url.openConnection().getContentLength();
		log.info("Downloading " + file.getName() + " (" + size / 1024 + "kb) ...");
		final InputStream in = url.openStream();
		final OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		final byte[] buffer = new byte[1024];
		int len, downloaded = 0, msgs = 0;
		final long start = System.currentTimeMillis();
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
			downloaded += len;
			if ((int)((System.currentTimeMillis() - start) / 500) > msgs) {
				log.info((int)((double)downloaded / (double)size * 100d) + "%");
				msgs++;
			}
		}
		in.close();
		out.close();
		log.info("Download finished");
	}

	public static void downloadIfNotExists(Logger log, File file, URL url) throws IOException {
		if (!file.exists() || file.length() == 0)
			Utils.download(log, url, file);
		if (!file.exists() || file.length() == 0)
			throw new FileNotFoundException(file.getAbsolutePath() + file.getName());
	}

	public static String readURL(URL url) throws IOException {
		final StringBuilder content = new StringBuilder();
		final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null)
			content.append(inputLine);
		in.close();
		return content.toString();
	}

	public static boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (final NumberFormatException ex) {}
		return false;
	}

	public static int parseTimeSpec(String[] spec) {
		if (spec == null || spec.length < 1 || spec.length > 2)
			return -1;
		if (!spec[0].contains(":") && !spec[0].contains("."))
			if (spec.length == 2) {
				if (!isInt(spec[0]))
					return -1;
				int min = Integer.parseInt(spec[0]);
				if (spec[1].startsWith("h"))
					min *= 60;
				else if (spec[1].startsWith("d"))
					min *= 1440;
				return min;
			} else if (spec.length == 1) {
				int days = 0, hours = 0, minutes = 0;
				int lastIndex = 0, currIndex = 1;
				while (currIndex <= spec[0].length()) {
					while (currIndex <= spec[0].length() && isInt(spec[0].substring(lastIndex, currIndex)))
						currIndex++;
					if (currIndex - 1 != lastIndex) {
						final String param = spec[0].substring(currIndex - 1, currIndex).toLowerCase();
						if (param.equals("d"))
							days = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
						else if (param.equals("h"))
							hours = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
						else if (param.equals("m"))
							minutes = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
					}
					lastIndex = currIndex;
					currIndex++;
				}
				if (days == 0 && hours == 0 && minutes == 0)
					return -1;
				return minutes + hours * 60 + days * 1440;
			} else
				return -1;
		final String timestamp;
		if (spec.length == 1) {
			if (spec[0].contains(":"))
				timestamp = new SimpleDateFormat("dd.MM.yyyy").format(System.currentTimeMillis()) + " " + spec[0];
			else
				timestamp = spec[0] + " 00:00:00";
		} else
			timestamp = spec[0] + " " + spec[1];
		try {
			return (int)((System.currentTimeMillis() - new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(timestamp).getTime()) / 60000);
		} catch (final ParseException ex) {
			return -1;
		}
	}
}
