package de.diddiz.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class Utils
{
	public static String newline = System.getProperty("line.separator");

	public static boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (final NumberFormatException ex) {
		}
		return false;
	}

	public static boolean isByte(String str) {
		try {
			Byte.parseByte(str);
			return true;
		} catch (final NumberFormatException ex) {
		}
		return false;
	}

	public static String listing(String[] entries, String delimiter, String finalDelimiter) {
		final int len = entries.length;
		if (len == 0)
			return "";
		if (len == 1)
			return entries[0];
		final StringBuilder builder = new StringBuilder(entries[0]);
		for (int i = 1; i < len - 1; i++)
			builder.append(delimiter).append(entries[i]);
		builder.append(finalDelimiter).append(entries[len - 1]);
		return builder.toString();
	}

	public static String listing(List<?> entries, String delimiter, String finalDelimiter) {
		final int len = entries.size();
		if (len == 0)
			return "";
		if (len == 1)
			return entries.get(0).toString();
		final StringBuilder builder = new StringBuilder(entries.get(0).toString());
		for (int i = 1; i < len - 1; i++)
			builder.append(delimiter).append(entries.get(i).toString());
		builder.append(finalDelimiter).append(entries.get(len - 1).toString());
		return builder.toString();
	}

	public static int parseTimeSpec(String[] spec) {
		if (spec == null || spec.length < 1 || spec.length > 2)
			return -1;
		if (spec.length == 1 && isInt(spec[0]))
			return Integer.valueOf(spec[0]);
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

	public static String spaces(int count) {
		final StringBuilder filled = new StringBuilder(count);
		for (int i = 0; i < count; i++)
			filled.append(' ');
		return filled.toString();
	}

	public static String join(String[] s, String delimiter) {
		if (s == null || s.length == 0)
			return "";
		final int len = s.length;
		final StringBuilder builder = new StringBuilder(s[0]);
		for (int i = 1; i < len; i++)
			builder.append(delimiter).append(s[i]);
		return builder.toString();
	}

	public static class ExtensionFilenameFilter implements FilenameFilter
	{
		private final String ext;

		public ExtensionFilenameFilter(String ext) {
			this.ext = ext;
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(ext);
		}
	}
}
