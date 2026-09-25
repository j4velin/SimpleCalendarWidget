package de.j4velin.calendarWidget.settings;

import java.util.HashSet;

class CalendarSet {

	private final HashSet<Integer> ids;

	CalendarSet(final String ids, final int size) {
		if (ids == null) {
			this.ids = new HashSet<Integer>(size);
		} else {
			final String[] id_strings = ids.split(",");
			this.ids = new HashSet<Integer>(id_strings.length);
			for (String id : id_strings) {
				this.ids.add(Integer.parseInt(id));
			}
		}
	}

	void add(final String id) {
		this.ids.add(Integer.parseInt(id));
	}
	
	void remove(final String id) {
		this.ids.remove(Integer.parseInt(id));
	}

	boolean contains(final String id) {
		return this.ids.contains(Integer.parseInt(id));
	}
	
	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		if (ids.isEmpty()) { return null; }
		for(Integer id : ids) {
			str.append(id.toString()).append(",");
		}
		return str.deleteCharAt(str.length()-1).toString();
	}

}
