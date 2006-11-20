package org.apache.jackrabbit.browser.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.util.ISO8601;

public class DateTimeToISO8601 implements Command {

	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private DateFormat timeFormat = new SimpleDateFormat("hh:mm aa");

	private String toKey = "iso8601";

	private String dateKey = "date";

	private String timeKey = "time";

	public boolean execute(Context ctx) throws Exception {
		String dateStr = (String) ctx.get(this.dateKey);
		String timeStr = (String) ctx.get(this.timeKey);

		Date date = dateFormat.parse(dateStr);
		Calendar dateCalendar = Calendar.getInstance();
		dateCalendar.setTime(date);

		Date time = timeFormat.parse(timeStr);
		Calendar timeCalendar = Calendar.getInstance();
		timeCalendar.setTime(time);

		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
		c.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
		c.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));

		c.set(Calendar.HOUR, timeCalendar.get(Calendar.HOUR));
		c.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
		c.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
		c.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND));

		ctx.put(this.toKey, ISO8601.format(c));

		return false;
	}

	/**
	 * @return the dateKey
	 */
	public String getDateKey() {
		return dateKey;
	}

	/**
	 * @param dateKey
	 *            the dateKey to set
	 */
	public void setDateKey(String dateKey) {
		this.dateKey = dateKey;
	}

	/**
	 * @return the timeKey
	 */
	public String getTimeKey() {
		return timeKey;
	}

	/**
	 * @param timeKey
	 *            the timeKey to set
	 */
	public void setTimeKey(String timeKey) {
		this.timeKey = timeKey;
	}

	/**
	 * @return the toKey
	 */
	public String getToKey() {
		return toKey;
	}

	/**
	 * @param toKey
	 *            the toKey to set
	 */
	public void setToKey(String toKey) {
		this.toKey = toKey;
	}

}
