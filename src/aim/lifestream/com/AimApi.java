package aim.lifestream.com;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class AimApi {

    private HttpParams httpClientParams;
    private DefaultHttpClient defaultHttpClient;
    // private ClientConnectionManager httpClientCm;
    private long timeDrift = 0;
    private String token = null;
    private String sessionKey = null;

    public boolean SignOn(String username, String password) {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        AddBaseParams(parameters, true);
        parameters.add(new BasicNameValuePair("pwd", password));
        parameters.add(new BasicNameValuePair("s", username));
        parameters.add(new BasicNameValuePair("tokenType", "longterm"));

        HttpRequestBase base = Post("https://api.screenname.aol.com/auth/clientLogin", parameters);
        HttpResponse response = Execute(base);
        JSONObject object = Parse(response);
        try {
            
            JSONObject data = object.getJSONObject("response").getJSONObject("data");
            String sessionSecret = data.getString("sessionSecret");
            //map.put("expiresIn", data.getJSONObject("token").getString("expiresIn"));
            token = data.getJSONObject("token").getString("a");
            timeDrift = Long.parseLong(data.getString("hostTime")) - System.currentTimeMillis()/1000;
            sessionKey = setSessionKey(sessionSecret, password);
            return true;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    
    
    public void StartSession() {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        AddBaseParams(parameters, false);
        parameters.add(new BasicNameValuePair("events", "im"));

        HttpRequestBase base = GetSigned("http://api.oscar.aol.com/aim/startSession", parameters);
        HttpResponse response = Execute(base);
        JSONObject object;
        if (response.getStatusLine().getStatusCode() == 200) {
            object = Parse(response);
        }
    }
    
    public void GetAggregated() {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters = new ArrayList<NameValuePair>();
        AddBaseParams(parameters, false);
        HttpRequestBase base = GetSigned("http://api.oscar.aol.com/lifestream/getAggregated", parameters);
        HttpResponse response = Execute(base);
        JSONObject object;
        if (response.getStatusLine().getStatusCode() == 200) {
            object = Parse(response);
        }
    }
    
    
    
    /// Helpers
    
    public void AddBaseParams(List<NameValuePair> parameters, boolean login) {
        if(token == null) {
            //throw friendly error
        }
        
        parameters.add(new BasicNameValuePair("clientName", "aimStatusTest"));
        parameters.add(new BasicNameValuePair("clientVersion", "1.0.0.0"));
        String keyName = (login)?"devId":"k";
        parameters.add(new BasicNameValuePair(keyName, "ai1iu803FncGRd61"));
        parameters.add(new BasicNameValuePair("f", "json"));
        parameters.add(new BasicNameValuePair("events", "im"));
        parameters.add(new BasicNameValuePair("r", "0"));
        if(!login) {
            parameters.add(new BasicNameValuePair("ts", Long.toString(System.currentTimeMillis()/1000 + timeDrift)));
            parameters.add(new BasicNameValuePair("a", token));
        }
        
    }

    private void setupHttpClient() {
        // setup http manager

        httpClientParams = new BasicHttpParams();
        // Increase max total connection to 200
        ConnManagerParams.setMaxTotalConnections(httpClientParams, 200);
        // Increase default max connection per route to 20
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
        /*
         * // Increase max connections for localhost:80 to 50 HttpHost localhost
         * = new HttpHost("locahost", 80); connPerRoute.setMaxForRoute(new
         * HttpRoute(localhost), 50);
         */
        ConnManagerParams.setMaxConnectionsPerRoute(httpClientParams, connPerRoute);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        //httpClientCm = new ThreadSafeClientConnManager(httpClientParams, schemeRegistry);

        httpClientParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "gb2312");
        httpClientParams.setParameter(CoreProtocolPNames.USER_AGENT,
                "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        httpClientParams.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
        httpClientParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        httpClientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        httpClientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 100000);
        Collection<Header> headers = new ArrayList<Header>();
        // headers.add(new BasicHeader("Accept", "*/*"));
        // headers.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
        httpClientParams.setParameter(ClientPNames.DEFAULT_HEADERS, headers);
        //defaultHttpClient = new DefaultHttpClient(httpClientCm, httpClientParams);
        defaultHttpClient = new DefaultHttpClient(httpClientParams);
        defaultHttpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3, false));

    }

    private JSONObject Parse(HttpResponse response) {

        // catch all?
        if (response.getStatusLine().getStatusCode() != 200) {
            return null;
        }
        
        JSONObject object = null;
        try {
            object = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return object;
    }

    public long getTimeDrift() {
        return timeDrift;
    }

    public void setTimeDrift(long timeDrift) {
        this.timeDrift = timeDrift;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    private HttpResponse Execute(HttpRequestBase base) {
        try {
            return defaultHttpClient.execute(base);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
   
    @SuppressWarnings("unchecked")
    private HttpRequestBase GetSigned(String baseUrl, List<NameValuePair> parameters) {
        if(sessionKey == null) {
            // throw friendly error
        }
        // sort parameters
        Collections.sort(parameters, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((NameValuePair) (o1)).getName()).compareTo(((NameValuePair) (o2)).getName());
            }
        });

        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(sessionKey.getBytes(),
                "HmacSHA256");
        
        javax.crypto.Mac mac;
        try {
            String fullRequest = new String("GET&" + java.net.URLEncoder.encode(baseUrl, "UTF-8") + "&" + java.net.URLEncoder.encode(getParams(parameters), "UTF-8"));
            mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            String strKey = new String(Base64.encodeBase64(mac.doFinal(fullRequest.getBytes())));
            strKey.trim();
            parameters.add(new BasicNameValuePair("sig_sha256", java.net.URLEncoder.encode(strKey, "UTF-8")));
            return Get(baseUrl, parameters);
        } catch (Exception ex) {
        }
        return null;

    }
    
    private String getParams(List<NameValuePair> parameters) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (NameValuePair parameter : parameters) {
            if (!first) {
                
                sb.append('&');
            } else {
               first = false;
            }
            sb.append(parameter.toString());
         }
         return sb.toString();
    }
    
    private String GetUrl(String baseUrl, List<NameValuePair> parameters) {
        boolean hasParms = baseUrl.indexOf('?') >= 0;
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean first = true;
        if(!hasParms)
            sb.append('?');
        
        sb.append(getParams(parameters));
        return sb.toString();
        
    }

    private HttpRequestBase Get(String baseUrl, List<NameValuePair> parameters) {
        return new HttpGet(GetUrl(baseUrl, parameters));
    }
    
    private HttpRequestBase Post(String baseUrl, List<NameValuePair> parameters) {
        HttpPost post = new HttpPost(baseUrl);
        try {
            post.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return post;
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

    private String signRequest(String postType, String baseURL, String queryString, String sessionKey) {
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(sessionKey.getBytes(),
                "HmacSHA256");
        javax.crypto.Mac mac;
        try {
            String fullRequest = new String(postType + "&" + java.net.URLEncoder.encode(baseURL, "UTF-8") + "&"
                    + java.net.URLEncoder.encode(queryString.toString(), "UTF-8"));
            mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(keySpec);

            String strKey = new String(Base64.encodeBase64(mac.doFinal(fullRequest.getBytes())));
            strKey.trim();
            return queryString + "&sig_sha256=" + java.net.URLEncoder.encode(strKey, "UTF-8");
        } catch (Exception ex) {
        }
        return null;
    }

    private byte[] hmacSHA256(String fullRequest, String sessionKey) {
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
        for (int i = 0; i < key.length; ++i)
            paddedKey[i] = key[i];
        for (int i = key.length; i < paddedKey.length; ++i)
            paddedKey[i] = 0;

        // XOR (bitwise exclusive-OR) the padded key with 64 bytes of 0x36.
        // (ipad)
        for (int i = 0; i < block_size; ++i)
            paddedKey[i] ^= 0x36;

        // append the message bytes to the XOR'd paddedKey
        sha256MD.update(paddedKey);
        sha256MD.update(fullRequest.getBytes());
        // apply the hash function (create the "Inner Hash")
        byte[] hash = sha256MD.digest();
        sha256MD.reset();

        // XOR (bitwise exclusive-OR) the padded key with 64 bytes of 0x5c.
        // (opad)
        // don't forget to counter-act the first XOR above
        for (int i = 0; i < 64; ++i)
            paddedKey[i] ^= (0x36 ^ 0x5c);
        // append the inner hash bytes to the second XOR'd paddedKey
        sha256MD.update(paddedKey);
        sha256MD.update(hash);
        // apply the hash function (create the "Outer Hash")
        hash = sha256MD.digest();

        // return the byte array for the hash (return the digest value)
        return hash;
    }
    
}
