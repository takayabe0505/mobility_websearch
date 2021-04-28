package websearch_covidinfodemic_0329;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import jp.ac.ut.csis.pflow.geom.Mesh;

public class mesh_dynamicpop {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat TIME     = new SimpleDateFormat("HH:mm:ss");
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void runMeshPop(
			String startdate,
			String enddate,
			String gpspath,
			File out,
			Integer meshsize,
			HashSet<String> ids
			) throws ParseException, NumberFormatException, IOException {
		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			File gps1 = new File(gpspath+date_str+".tsv");
			if((gps1.exists()) && (gps1.length()>0) ) {
				getMeshPop_forday(gps1, meshsize, date_str, out, ids);
			}
			date = next_date;
		}
	}

	public static void getMeshPop_forday(
			File in,
			Integer meshsize,
			String date_str,
			File out,
			HashSet<String> ids
			) throws NumberFormatException, IOException, ParseException{
		HashMap<String, HashSet<String>> id_observeddatetime = new HashMap<String, HashSet<String>>();
		HashMap<String, HashMap<String, HashSet<String>>> mesh_datetime_ids = 
				new HashMap<String, HashMap<String, HashSet<String>>>();
		BufferedReader br1 = new BufferedReader(new FileReader(in));
		String line1 = null;
		while((line1=br1.readLine())!=null){
			try {
				String[] tokens = line1.split("\t");
				if(tokens.length>=7){
					String id_br1 = tokens[0];
					if(!id_br1.equals("null")){ if(id_br1.length()>0){
						if(tokens[4].length()>=10){
							Double lon = Double.parseDouble(tokens[3]);
							Double lat = Double.parseDouble(tokens[2]);
							if(ids.contains(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								String time = datetime.split(" ")[1];
								String timeslot = utils.time2slot(time);

								String yesno = "yes";
								if(id_observeddatetime.containsKey(id_br1)) {
									if(id_observeddatetime.get(id_br1).contains(timeslot)) {
										yesno = "no";
									}
								}

								if(yesno.equals("yes")) {
									
									if(id_observeddatetime.containsKey(id_br1)) {
										id_observeddatetime.get(id_br1).add(timeslot);
									}
									else {
										HashSet<String> tmp = new HashSet<String>();
										tmp.add(timeslot);
										id_observeddatetime.put(id_br1, tmp);
									}
									
									String meshcode = new Mesh(meshsize, lon, lat).getCode();
									if(mesh_datetime_ids.containsKey(meshcode)) {
										if(mesh_datetime_ids.get(meshcode).containsKey(timeslot)) {
											mesh_datetime_ids.get(meshcode).get(timeslot).add(id_br1);
										}
										else {
											HashSet<String> tmp = new HashSet<String>();
											tmp.add(id_br1);
											mesh_datetime_ids.get(meshcode).put(timeslot, tmp);
										}
									}
									else {
										HashSet<String> tmp = new HashSet<String>();
										tmp.add(id_br1);
										HashMap<String, HashSet<String>> tmp2 = new HashMap<String, HashSet<String>>();
										tmp2.put(timeslot, tmp);
										mesh_datetime_ids.put(meshcode, tmp2);
									}}}}}}}
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
		
		writeout(mesh_datetime_ids, id_observeddatetime, date_str, out);
		
		System.out.println("--- got dynamic population map"+date_str);
	}


	
	public static void writeout(
			HashMap<String, HashMap<String, HashSet<String>>> mesh_datetime_ids,
			HashMap<String, HashSet<String>> id_obstimeslots,
			String date_str,
			File out
			) throws IOException {
		Integer obs_totalids = id_obstimeslots.size(); // normalize by number of users observed in day 
		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(String mesh : mesh_datetime_ids.keySet()) {
			for(String timeslot : mesh_datetime_ids.get(mesh).keySet()) {
				String time = utils.slot2time(timeslot);
				Integer count = mesh_datetime_ids.get(mesh).get(timeslot).size();
				Double rate = 10000d * ( (double) count / (double) obs_totalids );
				bw.write(date_str+" "+time+","+mesh+","+String.valueOf(rate));
				bw.newLine();
			}	
		}
		bw.close();
	}
	
	
	
	
}
