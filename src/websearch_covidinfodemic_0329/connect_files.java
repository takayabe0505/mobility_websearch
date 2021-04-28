package websearch_covidinfodemic_0329;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

public class connect_files {

	public static HashMap<String, String> dummyIDs(
			File in, 
			File out
			) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		HashSet<String> ids = new HashSet<String>();
		while((line=br.readLine())!=null) {
			String id = line.split("\t")[0];
			ids.add(id);
		}
		br.close();

		HashMap<String, String> id_newid = new HashMap<String, String>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		Integer num = 1;
		for(String id : ids) {
			bw.write(id+","+String.valueOf(num));
			id_newid.put(id, String.valueOf(num));
			bw.newLine();
			num+=1;
		}
		bw.close();

		return id_newid;
	}


	public static HashMap<String, Integer> ID_misinfoscore(
			File in, 
			File out
			) throws IOException {

		HashSet<String> words = new HashSet<String>();
		words.add("嘘");words.add("陰謀");words.add("テロ");words.add("兵器");words.add("デマ");
		words.add("フェイク");words.add("うそ");words.add("偽");

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		HashMap<String, Integer> id_score = new HashMap<String, Integer>();
		while((line=br.readLine())!=null) {
			String tokens[] = line.split("\t");
			String id = tokens[0];
			String word = tokens[1];
			if(words.contains(word)) {
				Integer count = Integer.valueOf(tokens[2]);
				if(id_score.containsKey(id)) {
					Integer newcount = count+id_score.get(id);
					id_score.put(id, newcount);
				}
				else {
					id_score.put(id, count);
				}
			}
		}
		br.close();

		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String id : id_score.keySet()) {
			bw.write(id+","+String.valueOf(id_score.get(id)));
			bw.newLine();
		}
		bw.close();

		return id_score;
	}



}
