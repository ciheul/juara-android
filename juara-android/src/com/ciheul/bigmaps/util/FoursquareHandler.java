package com.ciheul.bigmaps.util;

import java.util.HashMap;

import android.os.AsyncTask;
import android.util.Log;

public class FoursquareHandler {
    private final String baseRequestURI = "https://api.foursquare.com/v2/";
    private final String VERSION = "20130815";
    private final String CLIENT_ID = "PAC0HE1JPE2BNZLYFRVQPMHWQWSXPNKRGSNGVOZ2ATFINRKB";
    private final String CLIENT_SECRET = "3TBUWY0UCBB2PXQDYERHLGF3D1GZQDJB3GIRQ5UNVNIF4KIF";
    private Utility utility;

    public FoursquareHandler() {
        utility = new Utility();
        Log.d("BigMaps", "FoursquareHandler is created");
    }

    public void searchVenue(HashMap<String, Object> params) {
        String resourceURI = "venues/search/";
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("v", VERSION);
        String requestURI = Utility.constructURI(baseRequestURI, resourceURI, params);
        Log.d("BigMaps", "searchVenue: " + requestURI);
        new GetRequestTask().execute(requestURI);
    }

    private class GetRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return utility.sendHttpGetRequest(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("BigMaps", "onPostExecute: " + result);
        }

    }
}
