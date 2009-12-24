package aim.lifestream.com;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import javax.crypto.interfaces.*;
import java.security.NoSuchAlgorithmException;
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
        
        SignOn();
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
            	JSONObject obj = new JSONObject(fullResponse.toString());
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

	private byte[] hmacSHA256(String fullRequest, String sessionKey) 
	{
    	byte[] key = sessionKey.getBytes();
    	int block_size = 64;
    	
        // Get the SHA-256 Message Digest.
        MessageDigest sha256MD = null;
        try {
        	sha256MD = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
            System.err.println("This could be considered a problem. " + e.toString());
        }

        // if the key is bigger then the block size, hash it and use that
        if (key.length > block_size) {
        	sha256MD.update(key);
            key = sha256MD.digest();
            sha256MD.reset();
        }

        // If the key is less then the block size, pad it with zero bytes
        byte paddedKey[] = new byte[block_size];
        for (int i = 0; i < key.length; ++i) paddedKey[i] = key[i];
        for (int i = key.length; i < paddedKey.length; ++i) paddedKey[i] = 0;

        // XOR (bitwise exclusive-OR) the padded key with 64 bytes of 0x36. (ipad)
        for (int i = 0; i < block_size; ++i) paddedKey[i] ^= 0x36;
        
        //append the message bytes to the XOR'd paddedKey
        sha256MD.update(paddedKey);
        sha256MD.update(fullRequest.getBytes());
        //apply the hash function (create the "Inner Hash")
        byte[] hash = sha256MD.digest();
        sha256MD.reset();

        // XOR (bitwise exclusive-OR) the padded key with 64 bytes of 0x5c. (opad)
        // don't forget to counter-act the first XOR above
        for (int i = 0; i < 64; ++i) paddedKey[i] ^= (0x36 ^ 0x5c);
        // append the inner hash bytes to the second XOR'd paddedKey
        sha256MD.update(paddedKey);
        sha256MD.update(hash);
        //apply the hash function (create the "Outer Hash")
        hash = sha256MD.digest(); 
        
        //return the byte array for the hash (return the digest value)
        return hash;
	}
}