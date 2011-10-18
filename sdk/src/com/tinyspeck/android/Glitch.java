package com.tinyspeck.android;

import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;


public class Glitch {
	
	//// Strings used for authentication and requests ////
	
    public static final String BASE_URL = "http://api.glitch.com"; // Base service URL

    public String accessToken = null; // Access token for the currently logged in user
    
    private String redirectUri = null; // Redirect URI for OAuth flow 
    private String clientId = null; // Client Id for OAuth flow
    
    
    // Constructor for Glitch object
    // API Key is required for generating an auth token
    // Redirect URI is required for OAuth flow
    public Glitch(String apiKey, String uri)
    {
    	if (apiKey == null || uri == null)
    	{
    		throw new IllegalArgumentException(
                    "Please specify your API key and Redirect URI when initializing a Glitch object");
    	}
    	
    	this.clientId = apiKey;
    	this.redirectUri = uri;
    }
    
    
    //// Interacting with the API ////
    
    public GlitchRequest getRequest(String method)
    {
		return getRequest(method, null);
    }
    
    public GlitchRequest getRequest(String method, Map<String,String> params)
    {
		return new GlitchRequest(method, params, this);
    }


    //// Authorization ////
    
    // Start browser with authorize URL so the user can authorize the app with OAuth
    public void authorize(Context context, String scope, GlitchSessionDelegate delegate) {
        
        SharedPreferences prefs = context.getSharedPreferences("glitchskills.auth", Context.MODE_PRIVATE);
        if (prefs.contains("redirectUri"))
        {
            handleRedirect(context, Uri.parse(prefs.getString("redirectUri", null)), delegate);
            return;
        }
        
    	Uri authorizeUri = getAuthorizeUri(scope);
    	
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, authorizeUri);
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(browserIntent);
    }
    
    public void handleRedirect(Context context, Uri uri, GlitchSessionDelegate delegate)
    {
    	if (uri != null) {
        	// Get access token from URI fragment
            String fragment = uri.getFragment();
            
            String token = Glitch.extractTokenFromFragment(fragment);
            
            if (token != null)
            {
            	this.accessToken = token;
            	
                SharedPreferences prefs = context.getSharedPreferences("glitchskills.auth", Context.MODE_PRIVATE);
                prefs.edit().putString("redirectUri", uri.toString()).commit();
            	
            	delegate.glitchLoginSuccess();
            }
        }
    }
    
    public boolean isAuthenticated()
    {
    	return this.accessToken != null;
    }
    
    
    //// Authorization URI Creation ////
    
    public Uri getAuthorizeUri(String scope) {
    	return this.getAuthorizeUri(scope, null);
    }
    
    public Uri getAuthorizeUri(String scope, String state) {
    	scope = scope == null ? "identity" : scope;
    	
    	String url = this.getAuthUrl(scope, state);
    	
    	return Uri.parse(url);
    }
    
    public String getAuthUrl(String scope, String state){
    	
    	String authUrl = BASE_URL + "/oauth2/authorize?response_type=token&client_id=" + this.clientId + "&scope=" + scope + "&redirect_uri=" + this.redirectUri;
    	
    	if (state != null)
    	{
    		authUrl = authUrl + "&state=" + state;
    	}
    	
        return authUrl;
    }
    
    
    //// Static Helper Methods ////
    
    private static String extractTokenFromFragment(String fragment) {
    	String[] vars = fragment.split("&");
        for (int i = 0; i < vars.length; i++) {
            String[] param = vars[i].split("=");

            if (param.length == 2 && param[0].equals("access_token")) {
                return param[1];
            }
        }
        
        return null;
    }
}