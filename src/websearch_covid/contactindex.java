package websearch_covid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.Mesh;

public class contactindex {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	//	public static void main(String[] args) throws ParseException, IOException {
	//
	//		//		String gpspath = "/mnt/log/covid/loc/";
	//		String userpath = "/mnt/log/covid/user/";
	//
	//		/**
	//		 * @PARAMETERS
	//		 * - meshsize 
	//		 */
	//		String meshsize = "6";
	//		String city = "tokyo";
	//
	//		System.out.println("======== Starting "+city+" ========");
	//
	//		String year = "2020";
	//		String startdate = year+"0201";
	//		String enddate   = "20200716";
	//
	//		String citylabel = city;
	//
	//		/// write into my directory
	//		//		String rootpath = "/home/t-tyabe/COVID_websearch/"; 
	//		String rootpath = "/mnt/COVID_websearch_tyabe/"; 
	//		File rootfile = new File(rootpath); if(!rootfile.exists()) { rootfile.mkdir(); }
	//		String thispath = rootpath+citylabel+"/"; 
	//		File thisfile = new File(thispath); if(!thisfile.exists()) { thisfile.mkdir(); }
	//
	//		String idpath  = thispath+"idhomes/"; File idfilepath = new File(idpath); if(!idfilepath.exists()) { idfilepath.mkdir(); }
	//		String respath = thispath+"contactnetwork/"; File resdir = new File(respath); resdir.mkdir();
	//		String respath_fordays = thispath+"bydays/"; File resdir2 = new File(respath_fordays); resdir2.mkdir();
	//
	//		Double score_thres = Double.parseDouble(args[0]);
	//
	//		File SCI_target = new File(respath+"SCI_"+String.valueOf((int)Math.round(score_thres))+"_target.csv");
	//		File SCI_dummy  = new File(respath+"SCI_"+String.valueOf((int)Math.round(score_thres))+"_dummy.csv");
	//
	//		Date start_date_date = DATE.parse(startdate);
	//		Date end_date_date   = DATE.parse(enddate);
	//		Date date = start_date_date;
	//		while(date.before(end_date_date)){
	//
	//			String date_str = DATE.format(date);
	//			Date next_date = getresidents.nextday_date(date);
	//
	//			File idlist = new File(idpath+"ids_"+date_str+".csv");
	//			HashMap<String, String> targetids_mesh = gettargetids(idlist, meshsize);
	//			System.out.println("got target ids: "+String.valueOf(targetids_mesh.size()));
	//
	//			File COVIDusers_file = new File(userpath+date_str+"_user.tsv");
	//			HashSet<String> COVIDusers = getCOVIDids_hardthreshold(COVIDusers_file, score_thres);
	//			System.out.println("COVID users: "+String.valueOf(COVIDusers.size()));
	//
	//			Integer crosscheckIDs = crosscheck(COVIDusers, targetids_mesh);
	//			HashSet<String> dummyIDs = getdummyIDs(targetids_mesh, crosscheckIDs);
	//			System.out.println("cross check of users: "+String.valueOf(crosscheck(COVIDusers, targetids_mesh))+
	//					", dummy: "+String.valueOf(dummyIDs.size()));
	//
	//			File tmpout = new File(respath_fordays+date_str+"time_mesh_number_mesh"+meshsize+"_nohome.csv");
	//			// for each target user, compute how many contacts are co-located -> including / not including home area
	//			HashMap<String, HashMap<String, HashSet<String>>> data = getdata(tmpout);
	//			System.out.println("amount of data: "+String.valueOf(data.size()));
	//
	//			time_aggr(date_str,targetids_mesh,COVIDusers,data,SCI_target);
	//			time_aggr(date_str,targetids_mesh,dummyIDs,data,SCI_dummy);
	//			System.out.println("finished writing out aggregated measures: target");
	//
	//			System.out.println("======== "+city+" done "+date_str);
	//			date = next_date;
	//		}
	//	}
	//	//	}

	public static HashSet<String> getdummyIDs(HashMap<String, String> targetids_mesh, Integer crosscheckids) {
		HashSet<String> ids = new HashSet<String>();
		Integer c = 0;
		for(String id : targetids_mesh.keySet()) {
			if(c<crosscheckids) {
				ids.add(id);
			}
			else {
				break;
			}
		}
		return ids;
	}

	public static Integer crosscheck(HashSet<String> covidusers, HashMap<String, String> targetids) {
		Integer c = 0;
		for(String id1 : covidusers) {
			if(targetids.containsKey(id1)) {
				c+=1;
			}
		}
		return c;
	}

	public static Double percentile(ArrayList<Double> latencies, double percentile) {
		Collections.sort(latencies);
		int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
		return latencies.get(index-1);
	}

	public static HashSet<String> getCOVIDids_hardthreshold(
			File in, 
			Double thres
			) throws NumberFormatException, IOException{
		HashSet<String> covidusers = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split("\t");
			String id = tokens[0];
			Double score = Double.parseDouble(tokens[1]);
			if(score>thres) {
				covidusers.add(id);
			}
		}
		br.close();
		return covidusers;
	}

	public static HashSet<String> getCOVIDids_hardthreshold_multi(
			String userpath,
			String date_str,
			Integer days_sakanoboru,
			Double thres
			) throws NumberFormatException, IOException, ParseException{
		Date thisdate = DATE.parse(date_str);
		HashSet<String> covidusers = new HashSet<String>();
		for(Integer i=0; i<days_sakanoboru; i++) { // さかのぼる日数
			File COVIDusers_file = new File(userpath+date_str+"_user.tsv");
			if(i!=0) {
				Date saka_date = gobackdays_date(thisdate, days_sakanoboru);
				String saka_date_str = DATE.format(saka_date);
				COVIDusers_file = new File(userpath+saka_date_str+"_user.tsv");
			}
			if(COVIDusers_file.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(COVIDusers_file));
				String line = null;
				while((line=br.readLine())!= null) {
					String[] tokens = line.split("\t");
					String id = tokens[0];
					Double score = Double.parseDouble(tokens[1]);
					if(score>=thres) {
						covidusers.add(id);
					}
				}
				br.close();
			}
		}
		return covidusers;
	}

	public static Date gobackdays_date(Date day, Integer days) throws ParseException{
		Calendar nextCal = Calendar.getInstance();
		nextCal.setTime(day);
		nextCal.add(Calendar.DAY_OF_MONTH, -days);
		Date nextDate = nextCal.getTime();
		return nextDate;
	}

	public static HashSet<String> getCOVIDids_percentile(
			File in,
			Double percent
			) throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		ArrayList<Double> list = new ArrayList<Double>();
		while((line=br.readLine())!= null) {
			String[] tokens = line.split("\t");
			//			String id = tokens[0];
			Double score = Double.parseDouble(tokens[1]);
			list.add(score);
		}
		br.close();

		Double thres = percentile(list, percent);
		HashSet<String> covidusers = getCOVIDids_hardthreshold(in, thres);
		return covidusers;
	}


	public static HashMap<String, String> gettargetids(File in, String meshsize) throws IOException{
		HashMap<String, String> ids = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split(",");
			String id = tokens[0];
			String mesh = new Mesh(Integer.valueOf(meshsize), Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2])).getCode();
			ids.put(id, mesh);
		}
		br.close();
		return ids;
	}

	public static HashMap<String, HashMap<String, HashSet<String>>> getdata(File in) throws IOException{
		HashMap<String, HashMap<String, HashSet<String>>> data = new HashMap<String, HashMap<String, HashSet<String>>>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split("\t");
			String timeslot = tokens[0];
			String meshcode = tokens[1];
			String ids = tokens[2];
			if(data.containsKey(timeslot)) {
				if(data.get(timeslot).containsKey(meshcode)) {
					for(String id : ids.split(",")) {
						data.get(timeslot).get(meshcode).add(id);
					}
				}
				else {
					HashSet<String> tmp = new HashSet<String>();
					for(String id : ids.split(",")) {
						tmp.add(id);
					}
					data.get(timeslot).put(meshcode, tmp);
				}
			}
			else {
				HashSet<String> tmp = new HashSet<String>();
				for(String id : ids.split(",")) {
					tmp.add(id);
				}
				HashMap<String, HashSet<String>> tmp2 = new HashMap<String, HashSet<String>>();
				tmp2.put(meshcode, tmp);
				data.put(timeslot, tmp2);
			}
		}
		br.close();
		return data;
	}

	public static HashMap<String, HashMap<String, HashSet<String>>> gettargetlogs(
			String gpspath,
			HashMap<String, LonLat> ids_inside,
			String date_str, 
			String meshsize
			) throws NumberFormatException, IOException, ParseException{
		File in = new File(gpspath+"gps_"+date_str+".tar.gz");
		HashMap<String, HashMap<String, HashSet<String>>> dt_mesh_ids = new HashMap<String, HashMap<String, HashSet<String>>>();
		if(in.exists()) { if(in.length()>0) { 
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(in));
			BufferedReader br1 = new BufferedReader(new InputStreamReader(gzip));
			String line1 = null;
			while((line1=br1.readLine())!=null){
				try {
					String[] tokens = line1.split("\t");
					if(tokens.length>=7){
						String id_br1 = tokens[0];
						if(!id_br1.equals("null")){ if(id_br1.length()>0){
							if(ids_inside.containsKey(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								Integer hour = Integer.valueOf(datetime.split(" ")[1].substring(0, 2));
								Integer mins = Integer.valueOf(datetime.split(" ")[1].substring(2, 4));
								Integer half = 0;
								if(mins>=30) {
									half = 1;
								}
								String timeslot = String.valueOf(hour*2+half);
								Double lon = Double.parseDouble(tokens[3]);
								Double lat = Double.parseDouble(tokens[2]);
								String mesh = new Mesh(Integer.valueOf(meshsize), lon, lat).getCode();
								if(dt_mesh_ids.containsKey(timeslot)) {
									if(dt_mesh_ids.get(timeslot).containsKey(mesh)) {
										dt_mesh_ids.get(timeslot).get(mesh).add(id_br1);
									}
									else{
										HashSet<String> tmp = new HashSet<String>();
										tmp.add(id_br1);
										dt_mesh_ids.get(timeslot).put(mesh, tmp);
									}
								}
								else {
									HashSet<String> tmp = new HashSet<String>();
									tmp.add(id_br1);
									HashMap<String, HashSet<String>> tmp2 = new HashMap<String, HashSet<String>>();
									tmp2.put(mesh, tmp);
									dt_mesh_ids.put(timeslot, tmp2);
								}}}}}
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
			gzip.close();
		}}
		return dt_mesh_ids;
	}


	public static HashMap<String, HashMap<String, HashSet<String>>> getotherlogs(
			String gpspath,
			HashMap<String, HashMap<String, HashSet<String>>> dt_mesh_ids,
			HashMap<String, LonLat> targetids,
			String date_str, 
			String meshsize
			) throws NumberFormatException, IOException, ParseException{
		File in = new File(gpspath+"gps_"+date_str+".tar.gz");
		if(in.exists()) { if(in.length()>0) { 
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(in));
			BufferedReader br1 = new BufferedReader(new InputStreamReader(gzip));
			String line1 = null;
			while((line1=br1.readLine())!=null){
				try {
					String[] tokens = line1.split("\t");
					if(tokens.length>=7){
						String id_br1 = tokens[0];
						if(!id_br1.equals("null")){ if(id_br1.length()>0){
							if(!targetids.containsKey(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								Integer hour = Integer.valueOf(datetime.split(" ")[1].substring(0, 2));
								Integer mins = Integer.valueOf(datetime.split(" ")[1].substring(2, 4));
								Integer half = 0;
								if(mins>=30) {
									half = 1;
								}
								String timeslot = String.valueOf(hour*2+half);
								if(dt_mesh_ids.containsKey(timeslot)) {
									Double lon = Double.parseDouble(tokens[3]);
									Double lat = Double.parseDouble(tokens[2]);
									String mesh = new Mesh(Integer.valueOf(meshsize), lon, lat).getCode();
									if(dt_mesh_ids.get(timeslot).containsKey(mesh)) {
										dt_mesh_ids.get(timeslot).get(mesh).add(id_br1);
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
			gzip.close();
		}}
		return dt_mesh_ids;
	}


	public static void writeout(HashMap<String, HashMap<String, HashSet<String>>> data, File out) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String datetime: data.keySet()) {
			for(String mesh: data.get(datetime).keySet()) {
				bw.write(datetime+"\t"+mesh+"\t");
				for(String id: data.get(datetime).get(mesh)) {
					bw.write(id+",");
				}
				bw.newLine();
			}
		}
		bw.close();
	}

	//	public static void individual_aggr( //個々人で再集計する
	//			HashSet<String> targetids, 
	//			HashMap<String, HashMap<String, HashSet<String>>> data, 
	//			File out) {
	//		HashMap<String, HashMap<String, Integer>> id_time_count = new HashMap<String, HashMap<String, Integer>>();
	//		for(String targetid: targetids) {
	//			for(String ts : data.keySet()) {
	//				HashMap<String, HashSet<String>> tmp = data.get(ts);
	//				for(String mesh : tmp.keySet()) {
	//					if(tmp.get(mesh).contains(targetid)){
	//						Integer count = tmp.get(mesh).size();
	//						// input into result map
	//						if(id_time_count.containsKey(targetid)) {
	//							id_time_count.get(targetid).put(ts, count);
	//						}
	//						else {
	//							HashMap<String, Integer> time_count = new HashMap<String, Integer>();
	//							time_count.put(ts, count);
	//							id_time_count.put(targetid, time_count);
	//						}}}}}
	//		
	//	}


	////// ここを再設計する！
	public static void time_aggr( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data, 
			File out) throws IOException {
		HashMap<String, HashMap<String, Integer>> id_time_totalcount = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				HashSet<String> thistmp = tmp.get(mesh);
				//				Integer num = thistmp.size();
				// get number of IDs who are not home 
				Integer num = 0;
				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						String homemesh = targetids_mesh.get(id);
						if(!mesh.equals(homemesh)) { // if he is not home 
							num = num + 1;
						}
					}
					else {
						num = num + 1;
					}
				}

				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						if(covidusers.contains(id)) {
							String homemesh = targetids_mesh.get(id);
							if(!mesh.equals(homemesh)) { // if he is not home 
								if(id_time_totalcount.containsKey(id)) {
									id_time_totalcount.get(id).put(ts, num);
								}
								else {
									HashMap<String, Integer> aa = new HashMap<String, Integer>();
									aa.put(ts, num);
									id_time_totalcount.put(id, aa);
								}
							}}}}}}

		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(int t=0; t<48; t+=1) {
			Integer count = 0;
			Integer ids = 0;
			String ts = String.valueOf(t);
			for(String id: id_time_totalcount.keySet()) {
				if(id_time_totalcount.get(id).containsKey(ts)) {
					count = count + id_time_totalcount.get(id).get(ts);
					ids = ids + 1;
				}
			}
			String time = timeslot2time(ts);
			Double rate = (double)count/(double)ids;
			bw.write(date_str+" "+time+","+String.valueOf(count)+","+String.valueOf(ids)+","+String.valueOf(rate));
			bw.newLine();
		}
		bw.close();
	}

	public static String meshlevel2digits(String meshcode, Integer level) {
		String res = meshcode;
		if(level==6) { // 250m, 11 digits
			res = meshcode.substring(0,11);
		}
		else if(level==5) { // 250m, 10 digits
			res = meshcode.substring(0,10);
		}
		else if(level==4) { // 500m, 9 digits
			res = meshcode.substring(0,9);
		}
		else if(level==3) { // 1km, 8 digits
			res = meshcode.substring(0,8);
		}
		else if(level==2) { // 10km, 6 digits
			res = meshcode.substring(0,6);
		}
		else if(level==1) { // 80km, 4 digits
			res = meshcode.substring(0,4);
		}
		return res;
	}

	public static void time_aggr_allmesh_numids( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data,  // 
			File out_totalusers,
			LonLat max, LonLat min,
			Integer meshsize
			) throws IOException {
		HashMap<String, HashMap<String, Integer>> mesh_time_totalusers = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				String mesh_agg = meshlevel2digits(mesh, meshsize);
				LonLat c = new Mesh(mesh_agg).getCenter();
				if(getresidents.check_inside(c.getLon(), c.getLat(), max, min) == "yes") {
					HashSet<String> thistmp = tmp.get(mesh); // t=ts で メッシュ=mesh に滞在しているユーザID
					//				Integer num = thistmp.size();
					Integer num = 0; // t=ts で メッシュ=mesh に滞在＋自宅にいないユーザID数
					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							String homemesh = targetids_mesh.get(id);
							if(!mesh.equals(homemesh)) { // if he is not home 
								num = num + 1;
							}
						}
						else {
							num = num + 1;
						}
					}

					Integer usercount_all = 0;
					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							usercount_all+=1;
						}
					}

					if(mesh_time_totalusers.containsKey(mesh_agg)) {
						if(mesh_time_totalusers.get(mesh_agg).containsKey(ts)) {
							Integer tot = mesh_time_totalusers.get(mesh_agg).get(ts) + usercount_all;
							mesh_time_totalusers.get(mesh_agg).put(ts, tot);
						}
						else {
							mesh_time_totalusers.get(mesh_agg).put(ts, usercount_all);
						}
					}
					else {
						HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
						tmp2.put(ts, usercount_all);
						mesh_time_totalusers.put(mesh_agg, tmp2);
					}
				}
			}
		}

		writeout_res(date_str, out_totalusers, mesh_time_totalusers);

	}

	public static void time_aggr_allmesh_numids_limitmesh( //時間で再集計する
			String date_str,
			HashSet<String> meshlist,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data,  // 
			File out_totalusers,
			LonLat max, LonLat min,
			Integer meshsize
			) throws IOException {
		HashMap<String, HashMap<String, Integer>> mesh_time_totalusers = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				if(meshlist.contains(mesh.substring(0, 8))){
					String mesh_agg = meshlevel2digits(mesh, meshsize);
					LonLat c = new Mesh(mesh_agg).getCenter();
					if(getresidents.check_inside(c.getLon(), c.getLat(), max, min) == "yes") {
						HashSet<String> thistmp = tmp.get(mesh); // t=ts で メッシュ=mesh に滞在しているユーザID
						//				Integer num = thistmp.size();
						Integer num = 0; // t=ts で メッシュ=mesh に滞在＋自宅にいないユーザID数
						for(String id : thistmp) {
							if(targetids_mesh.containsKey(id)) {
								String homemesh = targetids_mesh.get(id);
								if(!mesh.equals(homemesh)) { // if he is not home 
									num = num + 1;
								}
							}
							else {
								num = num + 1;
							}
						}

						Integer usercount_all = 0;
						for(String id : thistmp) {
							if(targetids_mesh.containsKey(id)) {
								usercount_all+=1;
							}
						}

						if(mesh_time_totalusers.containsKey(mesh_agg)) {
							if(mesh_time_totalusers.get(mesh_agg).containsKey(ts)) {
								Integer tot = mesh_time_totalusers.get(mesh_agg).get(ts) + usercount_all;
								mesh_time_totalusers.get(mesh_agg).put(ts, tot);
							}
							else {
								mesh_time_totalusers.get(mesh_agg).put(ts, usercount_all);
							}
						}
						else {
							HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
							tmp2.put(ts, usercount_all);
							mesh_time_totalusers.put(mesh_agg, tmp2);
						}
					}
				}
			}
		}

		writeout_res(date_str, out_totalusers, mesh_time_totalusers);

	}


	public static void time_aggr_allmesh( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data,  // 
			File out_totalSCIcov,
			File out_totalSCI,
			File out_totalcovusers,
			File out_totalusers,
			LonLat max, LonLat min,
			Integer meshsize
			) throws IOException {
		HashMap<String, HashMap<String, Integer>> mesh_time_totalSCIcov   = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalSCI      = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalcovusers = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalusers = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				String mesh_agg = meshlevel2digits(mesh, meshsize);
				LonLat c = new Mesh(mesh_agg).getCenter();
				if(getresidents.check_inside(c.getLon(), c.getLat(), max, min) == "yes") {
					HashSet<String> thistmp = tmp.get(mesh); // t=ts で メッシュ=mesh に滞在しているユーザID
					//				Integer num = thistmp.size();
					Integer num = 0; // t=ts で メッシュ=mesh に滞在＋自宅にいないユーザID数
					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							String homemesh = targetids_mesh.get(id);
							if(!mesh.equals(homemesh)) { // if he is not home 
								num = num + 1;
							}
						}
						else {
							num = num + 1;
						}
					}

					Integer usercount = 0; // 
					Integer usercount_covid = 0; // 
					Integer usercount_all = 0;
					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							usercount+=1;
							String homemesh = targetids_mesh.get(id);
							if(!mesh.equals(homemesh)) { // if he is not home 
								usercount_all+=1;
								if(covidusers.contains(id)) {
									usercount_covid+=1;
								}}}
					}
					Integer SCI_covid = usercount_covid*num; // 
					Integer SCI_all = usercount_all*num;

					if(mesh_time_totalSCIcov.containsKey(mesh_agg)) {
						if(mesh_time_totalSCIcov.get(mesh_agg).containsKey(ts)) {
							Integer tot = mesh_time_totalSCIcov.get(mesh_agg).get(ts) + SCI_covid;
							mesh_time_totalSCIcov.get(mesh_agg).put(ts, tot);
						}
						else {
							mesh_time_totalSCIcov.get(mesh_agg).put(ts, SCI_covid);
						}
					}
					else {
						HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
						tmp2.put(ts, SCI_covid);
						mesh_time_totalSCIcov.put(mesh_agg, tmp2);
					}

					if(mesh_time_totalSCI.containsKey(mesh_agg)) {
						if(mesh_time_totalSCI.get(mesh_agg).containsKey(ts)) {
							Integer tot = mesh_time_totalSCI.get(mesh_agg).get(ts) + SCI_all;
							mesh_time_totalSCI.get(mesh_agg).put(ts, tot);
						}
						else {
							mesh_time_totalSCI.get(mesh_agg).put(ts, SCI_all);
						}
					}
					else {
						HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
						tmp2.put(ts, SCI_all);
						mesh_time_totalSCI.put(mesh_agg, tmp2);
					}

					if(mesh_time_totalcovusers.containsKey(mesh_agg)) {
						if(mesh_time_totalcovusers.get(mesh_agg).containsKey(ts)) {
							Integer tot = mesh_time_totalcovusers.get(mesh_agg).get(ts) + usercount_covid;
							mesh_time_totalcovusers.get(mesh_agg).put(ts, tot);
						}
						else {
							mesh_time_totalcovusers.get(mesh_agg).put(ts, usercount_covid);
						}
					}
					else {
						HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
						tmp2.put(ts, usercount_covid);
						mesh_time_totalcovusers.put(mesh_agg, tmp2);
					}

					if(mesh_time_totalusers.containsKey(mesh_agg)) {
						if(mesh_time_totalusers.get(mesh_agg).containsKey(ts)) {
							Integer tot = mesh_time_totalusers.get(mesh_agg).get(ts) + usercount;
							mesh_time_totalusers.get(mesh_agg).put(ts, tot);
						}
						else {
							mesh_time_totalusers.get(mesh_agg).put(ts, usercount);
						}
					}
					else {
						HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
						tmp2.put(ts, usercount);
						mesh_time_totalusers.put(mesh_agg, tmp2);
					}
				}
			}
		}

		writeout_res(date_str, out_totalSCIcov, mesh_time_totalSCIcov);
		writeout_res(date_str, out_totalSCI, mesh_time_totalSCI);
		writeout_res(date_str, out_totalcovusers, mesh_time_totalcovusers);
		writeout_res(date_str, out_totalusers, mesh_time_totalusers);
	}

	public static void time_aggr_allmesh_limitmesh( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data,  // 
			HashSet<String> meshlist, 
			File out_totalSCIcov,
			File out_totalSCI,
			File out_totalcovusers,
			File out_totalusers,
			LonLat max, LonLat min,
			Integer meshsize
			) throws IOException {
		HashMap<String, HashMap<String, Integer>> mesh_time_totalSCIcov   = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalSCI      = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalcovusers = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> mesh_time_totalusers = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				String mesh_agg = meshlevel2digits(mesh, meshsize);
				if(meshlist.contains(mesh.substring(0, 8))){
					LonLat c = new Mesh(mesh_agg).getCenter();
					if(getresidents.check_inside(c.getLon(), c.getLat(), max, min) == "yes") {
						HashSet<String> thistmp = tmp.get(mesh); // t=ts で メッシュ=mesh に滞在しているユーザID
						//				Integer num = thistmp.size();
						Integer num = 0; // t=ts で メッシュ=mesh に滞在＋自宅にいないユーザID数
						for(String id : thistmp) {
							if(targetids_mesh.containsKey(id)) {
								String homemesh = targetids_mesh.get(id);
								if(!mesh.equals(homemesh)) { // if he is not home 
									num = num + 1;
								}
							}
							else {
								num = num + 1;
							}
						}

						Integer usercount = 0; // 
						Integer usercount_covid = 0; // 
						Integer usercount_all = 0;
						for(String id : thistmp) {
							if(targetids_mesh.containsKey(id)) {
								usercount+=1;
								String homemesh = targetids_mesh.get(id);
								if(!mesh.equals(homemesh)) { // if he is not home 
									usercount_all+=1;
									if(covidusers.contains(id)) {
										usercount_covid+=1;
									}}}
						}
						Integer SCI_covid = usercount_covid*num; 
						Integer SCI_all = usercount_all*num;

						if(mesh_time_totalSCIcov.containsKey(mesh_agg)) {
							if(mesh_time_totalSCIcov.get(mesh_agg).containsKey(ts)) {
								Integer tot = mesh_time_totalSCIcov.get(mesh_agg).get(ts) + SCI_covid;
								mesh_time_totalSCIcov.get(mesh_agg).put(ts, tot);
							}
							else {
								mesh_time_totalSCIcov.get(mesh_agg).put(ts, SCI_covid);
							}
						}
						else {
							HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
							tmp2.put(ts, SCI_covid);
							mesh_time_totalSCIcov.put(mesh_agg, tmp2);
						}

						if(mesh_time_totalSCI.containsKey(mesh_agg)) {
							if(mesh_time_totalSCI.get(mesh_agg).containsKey(ts)) {
								Integer tot = mesh_time_totalSCI.get(mesh_agg).get(ts) + SCI_all;
								mesh_time_totalSCI.get(mesh_agg).put(ts, tot);
							}
							else {
								mesh_time_totalSCI.get(mesh_agg).put(ts, SCI_all);
							}
						}
						else {
							HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
							tmp2.put(ts, SCI_all);
							mesh_time_totalSCI.put(mesh_agg, tmp2);
						}

						if(mesh_time_totalcovusers.containsKey(mesh_agg)) {
							if(mesh_time_totalcovusers.get(mesh_agg).containsKey(ts)) {
								Integer tot = mesh_time_totalcovusers.get(mesh_agg).get(ts) + usercount_covid;
								mesh_time_totalcovusers.get(mesh_agg).put(ts, tot);
							}
							else {
								mesh_time_totalcovusers.get(mesh_agg).put(ts, usercount_covid);
							}
						}
						else {
							HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
							tmp2.put(ts, usercount_covid);
							mesh_time_totalcovusers.put(mesh_agg, tmp2);
						}

						if(mesh_time_totalusers.containsKey(mesh_agg)) {
							if(mesh_time_totalusers.get(mesh_agg).containsKey(ts)) {
								Integer tot = mesh_time_totalusers.get(mesh_agg).get(ts) + usercount;
								mesh_time_totalusers.get(mesh_agg).put(ts, tot);
							}
							else {
								mesh_time_totalusers.get(mesh_agg).put(ts, usercount);
							}
						}
						else {
							HashMap<String, Integer> tmp2 = new HashMap<String, Integer>();
							tmp2.put(ts, usercount);
							mesh_time_totalusers.put(mesh_agg, tmp2);
						}
					}}
			}
		}

		writeout_res(date_str, out_totalSCIcov, mesh_time_totalSCIcov);
		writeout_res(date_str, out_totalSCI, mesh_time_totalSCI);
		writeout_res(date_str, out_totalcovusers, mesh_time_totalcovusers);
		writeout_res(date_str, out_totalusers, mesh_time_totalusers);
	}

	public static void writeout_res(String date_str, File out, HashMap<String, HashMap<String, Integer>> data) throws IOException {
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(out,true));
		bw1.write("meshcode");
		for(int t=0; t<48; t+=1) {
			String time = timeslot2time(String.valueOf(t));
			bw1.write(","+date_str+" "+time);
		}
		bw1.newLine();
		for(String mesh : data.keySet()) {
			bw1.write(mesh);
			for(int t=0; t<48; t+=1) {
				Integer val = 0;
				String t_str = String.valueOf(t);
				if(data.get(mesh).containsKey(t_str)) {
					val = data.get(mesh).get(t_str);
				}
				bw1.write(","+String.valueOf(val));
			}
			bw1.newLine();
		}
		bw1.close();
	}

	public static String containsmesh(String mesh, HashSet<String>meshlist) {
		// meshlist meshes are shorter than mesh
		String res = "no";
		for(String m : meshlist) {
			if(mesh.contains(m)) {
				res = "yes";
				break;
			}
		}
		return res;
	}

	public static void time_aggr_thismesh( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashSet<String> meshlist,
			HashMap<String, HashMap<String, HashSet<String>>> data, 
			File out) throws IOException {
		HashMap<String, HashMap<String, Integer>> id_time_totalcount = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				if(containsmesh(mesh,meshlist).equals("yes")) {
					HashSet<String> thistmp = tmp.get(mesh);
					//				Integer num = thistmp.size();
					// get number of IDs who are not home 
					Integer num = 0;
					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							String homemesh = targetids_mesh.get(id);
							if(!mesh.equals(homemesh)) { // if he is not home 
								num = num + 1;
							}
						}
						else {
							num = num + 1;
						}
					}

					for(String id : thistmp) {
						if(targetids_mesh.containsKey(id)) {
							if(covidusers.contains(id)) {
								String homemesh = targetids_mesh.get(id);
								if(!mesh.equals(homemesh)) { // if he is not home 
									if(id_time_totalcount.containsKey(id)) {
										id_time_totalcount.get(id).put(ts, num);
									}
									else {
										HashMap<String, Integer> aa = new HashMap<String, Integer>();
										aa.put(ts, num);
										id_time_totalcount.put(id, aa);
									}
								}}}}}}}

		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(int t=0; t<48; t+=1) {
			Integer count = 0;
			Integer ids = 0;
			String ts = String.valueOf(t);
			for(String id: id_time_totalcount.keySet()) {
				if(id_time_totalcount.get(id).containsKey(ts)) {
					count = count + id_time_totalcount.get(id).get(ts);
					ids = ids + 1;
				}
			}
			String time = timeslot2time(ts);
			Double rate = (double)count/(double)ids;
			bw.write(date_str+" "+time+","+String.valueOf(count)+","+String.valueOf(ids)+","+String.valueOf(rate));
			bw.newLine();
		}
		bw.close();
	}



	public static void time_aggr_all( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashMap<String, HashMap<String, HashSet<String>>> data, 
			File out) throws IOException {
		HashMap<String, HashMap<String, Integer>> id_time_totalcount = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				HashSet<String> thistmp = tmp.get(mesh);
				//				Integer num = thistmp.size();
				// get number of IDs who are not home 
				Integer num = 0;
				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						String homemesh = targetids_mesh.get(id);
						if(!mesh.equals(homemesh)) { // if he is not home 
							num = num + 1;
						}
					}
					else {
						num = num + 1;
					}
				}

				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						String homemesh = targetids_mesh.get(id);
						if(!mesh.equals(homemesh)) { // if he is not home 
							if(id_time_totalcount.containsKey(id)) {
								id_time_totalcount.get(id).put(ts, num);
							}
							else {
								HashMap<String, Integer> aa = new HashMap<String, Integer>();
								aa.put(ts, num);
								id_time_totalcount.put(id, aa);
							}
						}}}}}

		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(int t=0; t<48; t+=1) {
			Integer count = 0;
			Integer ids = 0;
			String ts = String.valueOf(t);
			for(String id: id_time_totalcount.keySet()) {
				if(id_time_totalcount.get(id).containsKey(ts)) {
					count = count + id_time_totalcount.get(id).get(ts);
					ids = ids + 1;
				}
			}
			String time = timeslot2time(ts);
			Double rate = (double)count/(double)ids;
			bw.write(date_str+" "+time+","+String.valueOf(count)+","+String.valueOf(ids)+","+String.valueOf(rate));
			bw.newLine();
		}
		bw.close();
	}

	public static void time_aggr_random( //時間で再集計する
			String date_str,
			HashMap<String, String> targetids_mesh, 
			HashSet<String> covidusers,
			HashMap<String, HashMap<String, HashSet<String>>> data, 
			File out) throws IOException {
		HashMap<String, HashMap<String, Integer>> id_time_totalcount = new HashMap<String, HashMap<String, Integer>>();
		for(String ts : data.keySet()) {
			HashMap<String, HashSet<String>> tmp = data.get(ts);
			for(String mesh : tmp.keySet()) {
				HashSet<String> thistmp = tmp.get(mesh);
				//				Integer num = thistmp.size();
				// get number of IDs who are not home 
				Integer num = 0;
				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						String homemesh = targetids_mesh.get(id);
						if(!mesh.equals(homemesh)) { // if he is not home 
							num = num + 1;
						}
					}
					else {
						num = num + 1;
					}
				}

				Integer count = covidusers.size();
				for(String id : thistmp) {
					if(targetids_mesh.containsKey(id)) {
						String homemesh = targetids_mesh.get(id);
						if(!mesh.equals(homemesh)) { // if he is not home 
							if(id_time_totalcount.containsKey(id)) {
								id_time_totalcount.get(id).put(ts, num);
							}
							else {
								if(id_time_totalcount.size()<count) {
									HashMap<String, Integer> aa = new HashMap<String, Integer>();
									aa.put(ts, num);
									id_time_totalcount.put(id, aa);
								}
							}}}}}}

		Integer ids = targetids_mesh.size();
		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		for(int t=0; t<48; t+=1) {
			Integer count = 0;
			//			Integer ids = 0;
			String ts = String.valueOf(t);
			for(String id: id_time_totalcount.keySet()) {
				if(id_time_totalcount.get(id).containsKey(ts)) {
					count = count + id_time_totalcount.get(id).get(ts);
					//					ids = ids + 1;
				}
			}
			String time = timeslot2time(ts);
			Double rate = (double)count/(double)ids;
			bw.write(date_str+" "+time+","+String.valueOf(count)+","+String.valueOf(ids)+","+String.valueOf(rate));
			bw.newLine();
		}
		bw.close();
	}



	public static String timeslot2time(String timeslot) {
		Integer ts = Integer.valueOf(timeslot);
		String hour = String.format("%02d", ts/2);
		String mins = "00";
		if(ts%2==1) {
			mins = "30";
		}
		String res = hour+":"+mins+":00";
		return res;
	}


	public static String timeslot2datetime(String timeslot, String date) {
		Integer ts = Integer.valueOf(timeslot);
		String hour = String.format("%02d", ts/2);
		String mins = "00";
		if(ts%2==1) {
			mins = "30";
		}
		String res = date+" "+hour+":"+mins+":00";
		return res;
	}


}
