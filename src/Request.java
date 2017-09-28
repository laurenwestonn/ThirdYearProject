import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
public class Request {
	
	public static void main(String[] args) {
		try {
			Request.sendRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void sendRequest() throws Exception {
		
		String url = "https://maps.googleapis.com/maps/api/elevation/json?path=39.74,-105|39,-104&samples=10&key=AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		
		int responseCode = con.getResponseCode();
		
		System.out.println("\nSending 'GET' request to URL: " + url);
		System.out.println("Response Code: " + responseCode);
		
		BufferedReader in = new BufferedReader (
				new InputStreamReader(con.getInputStream()));
		
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		in.close();
		
		// Parse the JSON response
		Gson gson = new Gson();
		Response results = gson.fromJson(response.toString(), Response.class);
		
		for(Result r : results) {		
			System.out.println();	
			System.out.println("Elevation: " + r.elevation);
			System.out.println("Location: (" + r.location.lat + ", " + r.location.lng + ")");
		}
		
		

	}
	
	
}


























