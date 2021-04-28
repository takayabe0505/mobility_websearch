package websearch_covid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import jp.ac.ut.csis.pflow.geom.LonLat;
import parameters.cityinfo;

public class getresidents {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat TIME     = new SimpleDateFormat("HH:mm:ss");
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void main(String[] args) throws ParseException, IOException {

		String rootpath = "/home1/.../COVIDanalysis/"; File rootfile = new File(rootpath); if(!rootfile.exists()) { rootfile.mkdir(); }
		String gpspath = "/home1/bousai_data/";

		HashSet<String> cities = new HashSet<String>();
		cities.add("tokyo");

		for(String city: cities) {
			//			String city = args[0];
			System.out.println("======== Starting "+city+" ========");

			// spatial parameters
			ArrayList<LonLat> lonlats = cityinfo.getcityinfo(city);
			LonLat minp = lonlats.get(0);
			LonLat maxp = lonlats.get(1);

			//			String year = "2018";
			String year = "2019";

			String startdate = year+"1215";
			String enddate   = "20200401";

			String respath = rootpath+"idhomes/"; File resdir = new File(respath); resdir.mkdir();

			Date start_date_date = DATE.parse(startdate);
			Date end_date_date   = DATE.parse(enddate);
			Date date = start_date_date;
			while(date.before(end_date_date)){

				String date_str = DATE.format(date);
				Date next_date = nextday_date(date);

				if(date_str.contains(args[0])) {

					// at least one log in Tokyo area on this date 
					HashSet<String> ashikiriIDs = ashikiriIDs(minp,maxp,gpspath,respath,date); 
					System.out.println(" ... ashikiri: "+String.valueOf(ashikiriIDs.size())+" "+date_str);

					// get night time logs of above IDs on this day
					HashMap<String, HashMap<String, LonLat>> id_datetime_ll = getnightlogsofashikiriIDs(
							date, 
							gpspath, ashikiriIDs
							);

					// get ids that were in Tokyo on this day
					File out = new File(respath+"ids_"+date_str+".csv");
					//					HashMap<String, LonLat> finalids = 
					gethomelocs(id_datetime_ll, maxp, minp, out);
				}

				System.out.println("======== "+city+" done "+date_str);
				date = next_date;
			}
		}
	}


	public static HashSet<String> ashikiriIDs(
			LonLat minp, LonLat maxp,
			String gpspath, String respath,
			Date thisdate
			) throws ParseException, IOException {
		HashSet<String> ids = new HashSet<String>();
		String date_str = DATE.format(thisdate); 
		File gps1 = new File(gpspath+date_str+".tsv");
		if((gps1.exists()) && (gps1.length()>0)) {
			ashikiri(gps1,maxp,minp,ids);
		}
		return ids;
	}

	public static void ashikiri(
			File in1, 
			LonLat maxp, LonLat minp,
			HashSet<String> ashikiriIDs
			) throws NumberFormatException, IOException{
		BufferedReader br1 = new BufferedReader(new FileReader(in1));
		String line1 = null;
		while((line1=br1.readLine())!=null){
			try {
				String[] tokens = line1.split("\t");
				if(tokens.length>=7){
					String id_br1 = tokens[0];
					if(!id_br1.equals("null")){ if(id_br1.length()>1){
						if(!ashikiriIDs.contains(id_br1)) {
							if(!id_br1.contains("ktsubouc")) {
								Double lon = Double.parseDouble(tokens[3]);
								Double lat = Double.parseDouble(tokens[2]);
								if(check_inside(lon, lat, maxp, minp)=="yes") {
									String unixtime = tokens[4];
									Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
									DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
									String time = DATETIME.format(currentDate);
									Integer hour = Integer.valueOf(time.split(" ")[1].substring(0, 2));
									if(hour<12) {
										ashikiriIDs.add(id_br1);
									}
								}
							}
						}
					}}}
			}
			catch (ArrayIndexOutOfBoundsException  e){
				System.out.println("OUT OF BOUNDS EXCEPTION ----");
				System.out.println(line1);
				System.out.println("----");
			}
			catch (Exception  e){
				System.out.println("OTHER ERROR IN LINE ----");
				System.out.println(line1);
				System.out.println("----");				
			}
		}
		br1.close();
	}

	public static String check_inside(Double lon, Double lat, LonLat maxp, LonLat minp) {
		String res = "no";
		if((lon<maxp.getLon())&&(lon>minp.getLon())){
			if((lat<maxp.getLat())&&(lat>minp.getLat())){
				res = "yes";
			}
		}
		return res; 
	}

	public static String check(String flag, Integer hour, Double lon, Double lat, LonLat maxp, LonLat minp) {
		String res = "no";
		if(flag.equals("before")) {
			if(hour>=18){
				if((lon<maxp.getLon())&&(lon>minp.getLon())){
					if((lat<maxp.getLat())&&(lat>minp.getLat())){
						res = "yes";
					}
				}
			}
		}
		if(flag.equals("after")) {
			if(hour<18){
				if((lon<maxp.getLon())&&(lon>minp.getLon())){
					if((lat<maxp.getLat())&&(lat>minp.getLat())){
						res = "yes";
					}
				}
			}
		}
		return res; 
	}

	public static String check2(String flag, Integer hour, Double lon, Double lat) {
		String res = "no";
		if(flag.equals("before")) {
			if(hour>=18){
				res = "yes";
			}
		}
		if(flag.equals("after")) {
			if(hour<18){
				res = "yes";
			}
		}
		return res; 
	}

	public static Date nextday_date(Date day) throws ParseException{
		Calendar nextCal = Calendar.getInstance();
		nextCal.setTime(day);
		nextCal.add(Calendar.DAY_OF_MONTH, 1);
		Date nextDate = nextCal.getTime();
		return nextDate;
	}

	public static Date beforeday_date(Date day) throws ParseException{
		Calendar nextCal = Calendar.getInstance();
		nextCal.setTime(day);
		nextCal.add(Calendar.DAY_OF_MONTH, -1);
		Date nextDate = nextCal.getTime();
		return nextDate;
	}

	public static HashMap<String, HashMap<String, LonLat>> getnightlogsofashikiriIDs(
			Date thisdate,
			String gpspath,
			HashSet<String> ids
			) throws ParseException, NumberFormatException, IOException {
		HashMap<String, HashMap<String, LonLat>> id_datetime_ll = new HashMap<String, HashMap<String, LonLat>>();
		String date_str = DATE.format(thisdate);
		Date next_date = beforeday_date(thisdate);
		String date_next_str = DATE.format(next_date);
		File gps1 = new File(gpspath+date_str+".tsv");
		File gps2 = new File(gpspath+date_next_str+".tsv");
		if((gps1.exists()) && (gps1.length()>0) && (gps2.exists()) && (gps2.length()>0)) {
			getlogs(gps2, ids, date_str, id_datetime_ll, "before");
			getlogs(gps1, ids, date_str, id_datetime_ll, "after");
		}
		System.out.println("getlogs done "+String.valueOf(id_datetime_ll.size())+" "+date_str);
		return id_datetime_ll;
	}

	public static void getlogs(
			File in,
			HashSet<String> ids_inside,
			String date,
			HashMap<String, HashMap<String, LonLat>> id_datetime_ll,
			String flag
			) throws NumberFormatException, IOException, ParseException{
		BufferedReader br1 = new BufferedReader(new FileReader(in));
		String line1 = null;
		while((line1=br1.readLine())!=null){
			try {
				String[] tokens = line1.split("\t");
				if(tokens.length>=7){
					String id_br1 = tokens[0];
					if(!id_br1.equals("null")){ if(id_br1.length()>0){
//						if(tokens[4].length()>=18){
							Double lon = Double.parseDouble(tokens[3]);
							Double lat = Double.parseDouble(tokens[2]);
							LonLat p = new LonLat(lon,lat);
							if(ids_inside.contains(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								Integer hour = Integer.valueOf(datetime.split(" ")[1].substring(0, 2));
								String res = check2(flag,hour,lon,lat);
								if(res.equals("yes")) {
									if(id_datetime_ll.containsKey(id_br1)) {
										id_datetime_ll.get(id_br1).put(datetime, p);
									}
									else {
										HashMap<String, LonLat> tmp = new HashMap<String, LonLat>();
										tmp.put(datetime, p);
										id_datetime_ll.put(id_br1, tmp);
									}}}}}}
//				}
			}
			catch (ArrayIndexOutOfBoundsException  e){
				System.out.println("OUT OF BOUNDS EXCEPTION ----");
				System.out.println(line1);
				System.out.println("----");
			}
			catch (Exception  e){
				System.out.println("OTHER ERROR IN LINE ----");
				System.out.println(line1);
				System.out.println("----");				
			}
		}
		br1.close();
	}


	public static HashMap<String, LonLat> gethomelocs(
			HashMap<String, HashMap<String, LonLat>> id_datetime_ll,
			LonLat maxp, LonLat minp, File out
			) throws IOException{
		HashMap<String, LonLat> ids = new HashMap<String, LonLat>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for (String id : id_datetime_ll.keySet()) {
			HashMap<String, LonLat> datapoints = id_datetime_ll.get(id);
			LonLat avg = getavg(datapoints);
			Double lon = avg.getLon();
			Double lat = avg.getLat();
			if((lon<maxp.getLon())&&(lon>minp.getLon())){
				if((lat<maxp.getLat())&&(lat>minp.getLat())){
					ids.put(id, new LonLat(lon,lat));
					bw.write(id+","+
							String.valueOf(ids.get(id).getLon())+","+
							String.valueOf(ids.get(id).getLat())); 
					bw.newLine();
				}
			}
		}
		bw.close();
		return ids;
	}




	public static LonLat getavg(HashMap<String, LonLat> data_thisid) {
		Double tmplon = 0d;
		Double tmplat = 0d;
		for(String d : data_thisid.keySet()) {
			LonLat p = data_thisid.get(d);
			tmplon = tmplon + p.getLon();
			tmplat = tmplat + p.getLat();
		}
		Double lon = tmplon / (double)data_thisid.size(); 
		Double lat = tmplat / (double)data_thisid.size(); 
		LonLat avg = new LonLat(lon,lat);
		return avg;
	}


}
