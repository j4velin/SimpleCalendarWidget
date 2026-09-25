package de.j4velin.calendarWidget;

class Event {

    final long id;
    final long date;
    final String title;
    final String location;
    final boolean allDay;
    final long end;
    final int color;
    final boolean multiDay;
    final boolean multiDayIsOriginal;

    Event(final long id, final long date, final String title, final String location,
          final boolean allDay, final long end, final int eventColor, final int calendarColor,
          final boolean multiDay, final boolean isOriginal) {
        this.id = id;
        this.title = title;
        this.location = location == null || location.isEmpty() ? null : location;
        this.allDay = allDay;
        this.date = date;
        this.end = allDay ? end - 1 : end;
        this.color = eventColor != 0 ? eventColor : calendarColor;
        this.multiDay = multiDay;
        this.multiDayIsOriginal = isOriginal;
    }

}
