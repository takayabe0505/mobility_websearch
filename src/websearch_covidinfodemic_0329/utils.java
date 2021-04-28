package websearch_covidinfodemic_0329;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class utils {

	public static Date nextday_date(Date day) throws ParseException{
		Calendar nextCal = Calendar.getInstance();
		nextCal.setTime(day);
		nextCal.add(Calendar.DAY_OF_MONTH, 1);
		Date nextDate = nextCal.getTime();
		return nextDate;
	}
	
	public static String time2slot(String time) {
		Integer hour = 2*(Integer.parseInt(time.split(":")[0]));
		Integer mins = Integer.parseInt(time.split(":")[1]);
		if(mins>=30) {
			hour = hour + 1;
		}
		String res = String.valueOf(hour);
		return res; // 0~47 slot
	}

	public static String slot2time(String slot) {
		Integer hour = Integer.parseInt(slot)/2;
		Integer mins = (Integer.parseInt(slot) - 2*hour)*30;
		String res = String.valueOf(hour)+":"+String.valueOf(mins)+":00";
		return res; // hh:mm:00
	}
	
}
