package de.j4velin.calendarWidget;

import java.util.ArrayList;
import java.util.List;

public class Day {
	
	final long date;
	final List<Event> events = new ArrayList<Event>(3);

	Day(final long date) {
		this.date = date;
	}
}
