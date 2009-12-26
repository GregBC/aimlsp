package aim.lifestream.com;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.codec.binary.Base64;

public class Lifestream extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Capture our button from layout
        Button button = (Button)findViewById(R.id.cancel);
        // Register the onClick listener with the implementation above
        button.setOnClickListener(new Button.OnClickListener() { public void onClick (View v){ QuitApp(); }});
        
        Button signOnBtn = (Button)findViewById(R.id.ok);
        signOnBtn.setOnClickListener(new Button.OnClickListener() { public void onClick (View v){ SignOn(); }});
    }
    
    public void SignOn()
    {
		try {
	        // Construct data
	        String data = URLEncoder.encode("clientName", "UTF-8") + "=" + URLEncoder.encode("aimStatusTest", "UTF-8");
	        data += "&" + URLEncoder.encode("clientVersion", "UTF-8") + "=" + URLEncoder.encode("1.0.0.0", "UTF-8");
	        data += "&" + URLEncoder.encode("devId", "UTF-8") + "=" + URLEncoder.encode("ai1iu803FncGRd61", "UTF-8");
	        data += "&" + URLEncoder.encode("f", "UTF-8") + "=" + URLEncoder.encode("qs", "UTF-8");
	        data += "&" + URLEncoder.encode("pwd", "UTF-8") + "=" + URLEncoder.encode("maryland", "UTF-8");
	        data += "&" + URLEncoder.encode("r", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8");
	        data += "&" + URLEncoder.encode("s", "UTF-8") + "=" + URLEncoder.encode("gregtest6", "UTF-8");
	        data += "&" + URLEncoder.encode("tokenType", "UTF-8") + "=" + URLEncoder.encode("longterm", "UTF-8");
	        
	        // Send data
	        URL url = new URL("https://api.screenname.aol.com/auth/clientLogin");
	        URLConnection conn = url.openConnection();
	        conn.setDoOutput(true);
	        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	        wr.write(data);
	        wr.flush();
	    
	        // Get the response
	        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;
	        String params[];
	        String delimeter = "&";
	        while ((line = rd.readLine()) != null) {
	        	if (line.contains("statusCode=200"))
	        	{
	        		params = line.split(delimeter);
	        		Map<String, String> map = new HashMap<String, String>();
	        		for(int i =0; i < params.length ; i++)
	        		{
	        			String str = params[i];
	        			int equal = str.indexOf("=");
	        			String key = str.substring(0, equal);
	        			equal ++;
	        			String val = str.substring(equal);
	        			map.put(key, val);
	        		}
	        		
	        	    TextView tv = new TextView(this);
	        	    tv.setText("Client Login Success");
	        	    setContentView(tv);
	        	    StartSession(map, "maryland");
	        	}
	        	else
	        	{
	        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        		builder.setMessage("There was an error authenticating")
	        		       .setCancelable(false)
	        		       .setNegativeButton("OK", new DialogInterface.OnClickListener() {
	        		           public void onClick(DialogInterface dialog, int id) {
	        		                dialog.cancel();
	        		           }
	        		       });
	        		AlertDialog alert = builder.create();
	        		alert.show();
	        	}
	        }
	        wr.close();
	        rd.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void StartSession(Map <String, String> map, String password)
    {
		try {
			String sessionKey = setSessionKey((String)map.get("sessionSecret"), password);
			String token = (String)map.get("token_a");
			String hostTime = (String)map.get("hostTime");
	        String data = URLEncoder.encode("a", "UTF-8") + "=" + URLEncoder.encode(token, "UTF-8");
	        data += "&" + URLEncoder.encode("clientName", "UTF-8") + "=" + URLEncoder.encode("aimStatusTest", "UTF-8");
	        data += "&" + URLEncoder.encode("clientVersion", "UTF-8") + "=" + URLEncoder.encode("1.0.0.0", "UTF-8");
	        data += "&" + URLEncoder.encode("events", "UTF-8") + "=" + URLEncoder.encode("im", "UTF-8");
	        data += "&" + URLEncoder.encode("f", "UTF-8") + "=" + URLEncoder.encode("json", "UTF-8");
	        data += "&" + URLEncoder.encode("k", "UTF-8") + "=" + URLEncoder.encode("ai1iu803FncGRd61", "UTF-8");
	        data += "&" + URLEncoder.encode("r", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8");
	        data += "&" + URLEncoder.encode("ts", "UTF-8") + "=" + URLEncoder.encode(hostTime, "UTF-8");
	        
	        String signedParams = signRequest("GET", "http://api.oscar.aol.com/aim/startSession", data, sessionKey);
        	// Send data
	        String urlStr = "http://api.oscar.aol.com/aim/startSession?" + signedParams;
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);        
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder fullResponse = new StringBuilder();
            String line;
	        String params[];
	        String delimeter = "&";
            while ((line = rd.readLine()) != null) {
            	fullResponse.append(line);
            }
	        rd.close(); 
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private String setSessionKey(String sessionSecret, String password) {
		String bStr = "";
		
		javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(password.getBytes(), "HmacSHA256");
		javax.crypto.Mac mac;
		try {
			mac = javax.crypto.Mac.getInstance("HmacSHA256");
			mac.init(keySpec);

			String sessionKey = new String(Base64.encodeBase64(mac.doFinal(sessionSecret.getBytes())));
			return sessionKey.trim();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String signRequest(String postType, String baseURL, String queryString, String sessionKey)
	{
		javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(sessionKey.getBytes(), "HmacSHA256");
		javax.crypto.Mac mac;
		try {
			String fullRequest = new String(postType + "&" + java.net.URLEncoder.encode(baseURL, "UTF-8") + "&" + java.net.URLEncoder.encode(queryString.toString(), "UTF-8"));
			mac = javax.crypto.Mac.getInstance("HmacSHA256");
			mac.init(keySpec);

			String strKey = new String(Base64.encodeBase64(mac.doFinal(fullRequest.getBytes())));
			strKey.trim();
			return queryString + "&sig_sha256=" + java.net.URLEncoder.encode(strKey, "UTF-8");
		}catch(Exception ex){
		}
		return null;
	}
	
    public void QuitApp()
    {
    	this.finish();
    }
}