package parameters;

import java.util.ArrayList;

import jp.ac.ut.csis.pflow.geom.LonLat;

public class cityinfo {

	public static ArrayList<LonLat> getcityinfo(String city){
		ArrayList<LonLat> lonlats = new ArrayList<LonLat>();
		if(city.equals("tokyo")) {
//			LonLat minp = new LonLat(139.47d,35.59d);
			LonLat minp = new LonLat(139.28d,35.59d);
//			LonLat maxp = new LonLat(139.92d,35.83d);
			LonLat maxp = new LonLat(139.92d,35.93d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("osaka")) {
			LonLat minp = new LonLat(135.33d,34.48d);
			LonLat maxp = new LonLat(135.67d,34.85d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("kyoto")) {
			LonLat minp = new LonLat(135.65d,34.89d);
			LonLat maxp = new LonLat(135.85d,35.11d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("sapporo")) {
			LonLat minp = new LonLat(141.21d,42.95d);
			LonLat maxp = new LonLat(141.63d,43.27d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("kitakyushu")) {
			LonLat minp = new LonLat(130.67d,33.74d);
			LonLat maxp = new LonLat(131.06d,34d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("yokohama")) {
			LonLat minp = new LonLat(139.50d,35.36d);
			LonLat maxp = new LonLat(139.83d,35.62d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("fukuoka")) {
			LonLat minp = new LonLat(130.30d,33.48d);
			LonLat maxp = new LonLat(130.55d,33.74d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("nagoya")) {
			LonLat minp = new LonLat(136.74d,34.97d);
			LonLat maxp = new LonLat(137.19d,35.44d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("kumamoto")) {
			LonLat minp = new LonLat(130.48d,32.42d);
			LonLat maxp = new LonLat(130.95d,33.03d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("sendai")) {
			LonLat minp = new LonLat(140.71d,38.10d);
			LonLat maxp = new LonLat(141.08d,38.34d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("hiroshima")) {
//			LonLat minp = new LonLat(132.26d,34.22d);
			LonLat minp = new LonLat(132.26d,34.27d);
			LonLat maxp = new LonLat(132.60d,34.49d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("niigata")) {
//			LonLat minp = new LonLat(138.66d,37.50d);
			LonLat minp = new LonLat(138.73d,37.72d);
//			LonLat maxp = new LonLat(139.43d,38.16d);
			LonLat maxp = new LonLat(139.27d,38.02d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("kobe")) {
			LonLat minp = new LonLat(135.13d,34.62d);
//			LonLat maxp = new LonLat(135.31d,34.82d);
			LonLat maxp = new LonLat(135.31d,34.77d);
			lonlats.add(minp); lonlats.add(maxp);
		}
		else if(city.equals("okayama")) {
			LonLat minp = new LonLat(133.64d,34.45d);
			LonLat maxp = new LonLat(134.13d,34.80d);
			lonlats.add(minp); lonlats.add(maxp);
		}	
		else if(city.equals("test")) {
			LonLat minp = new LonLat(133.64d,34.45d);
			LonLat maxp = new LonLat(133.83d,34.55d);
			lonlats.add(minp); lonlats.add(maxp);
		}	
		else {
			System.out.println("###ERROR... Choose city from: "
					+ "{tokyo, osaka, kyoto, sapporo, yokohama, "
					+ "fukuoka, nagoya, kumamoto, sendai, "
					+ "hiroshima, niigata, kobe, okayama}"
					+ " (13 cities covered)");
		}
		System.out.println("You can also choose city from: "
				+ "{tokyo, osaka, kyoto, sapporo, yokohama, "
				+ "fukuoka, nagoya, kumamoto, sendai, "
				+ "hiroshima, niigata, kobe, okayama}"
				+ " (13 cities covered)");
		return lonlats;
	}

}
