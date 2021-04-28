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

public class user_SCI {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat TIME     = new SimpleDateFormat("HH:mm:ss");
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void runSCImetric(
			File dynamicpop,
			String startdate,
			String enddate,
			String gpspath,
			Integer meshsize,
			HashSet<String> ids,
			File out
			) throws ParseException, NumberFormatException, IOException {
		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			File gps1 = new File(gpspath+date_str+".tsv");
			if((gps1.exists()) && (gps1.length()>0) ) {
				getSCI_forday(gps1, dynamicpop, meshsize, date_str, ids, out);
			}
			date = next_date;
		}
	}


	public static void getSCI_forday(
			File gps1,
			File dynamicpop,
			Integer meshsize,
			String date_str,
			HashSet<String> ids,
			File out
			) throws NumberFormatException, IOException, ParseException {
		HashMap<String, HashMap<String, Double>> mesh_timeslot_density = getDensity(dynamicpop, date_str);
		getuserSCI(gps1,mesh_timeslot_density, meshsize, date_str, out, ids);
		System.out.println("--- got dynamic population map"+date_str);
	}


	public static HashMap<String, HashMap<String, Double>> getDensity(
			File in, 
			String date_str
			) throws IOException{
		HashMap<String, HashMap<String, Double>> mesh_timeslot_density = new HashMap<String, HashMap<String, Double>>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] tokens = line.split(",");
			String date = tokens[0].split(" ")[0];
			if(date.equals(date_str)) {
				String datetime = tokens[0].split(" ")[1];
				String timeslot = utils.time2slot(datetime);
				String meshcode = tokens[1];
				Double rate = Double.parseDouble(tokens[2]);
				if(mesh_timeslot_density.containsKey(meshcode)) {
					mesh_timeslot_density.get(meshcode).put(timeslot, rate);
				}
				else {
					HashMap<String, Double> tmp = new HashMap<String, Double>();
					tmp.put(timeslot, rate);
					mesh_timeslot_density.put(meshcode, tmp);
				}
			}
		}
		br.close();
		return mesh_timeslot_density;
	}


	public static void getuserSCI(
			File in,
			HashMap<String, HashMap<String, Double>> mesh_timeslot_density,
			Integer meshsize,
			String date_str,
			File out,
			HashSet<String> ids
			) throws NumberFormatException, IOException, ParseException{
		HashMap<String, HashMap<String, String>> id_datetime_meshcode = new HashMap<String, HashMap<String, String>>();
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

								String meshcode = new Mesh(meshsize, lon, lat).getCode();
								if(id_datetime_meshcode.containsKey(id_br1)) {
									id_datetime_meshcode.get(id_br1).put(timeslot, meshcode);
								}
								else {
									HashMap<String, String> tmp = new HashMap<String, String>();
									tmp.put(timeslot, meshcode);
									id_datetime_meshcode.put(id_br1, tmp);
								}}}}}}
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

		HashMap<String, Double> id_totalSCI = new HashMap<String, Double>();
		for(String id : id_datetime_meshcode.keySet()) {
			Double count = 0d;
			for(String timeslot : id_datetime_meshcode.get(id).keySet()) {
				String thismesh = id_datetime_meshcode.get(id).get(timeslot);
				if(mesh_timeslot_density.containsKey(thismesh)) {
					if(mesh_timeslot_density.get(thismesh).containsKey(timeslot)) {
						count = count + mesh_timeslot_density.get(thismesh).get(timeslot);
					}
				}
			}
			id_totalSCI.put(id, count);
		}

		writeout(id_totalSCI, date_str, out);
	}



	public static void writeout(
			HashMap<String, Double> id_totalSCI,
			String date_str,
			File out
			) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(String id : id_totalSCI.keySet()) {
			bw.write(id+","+date_str+","+String.valueOf(id_totalSCI.get(id)));
			bw.newLine();
		}
		bw.close();
	}

}


