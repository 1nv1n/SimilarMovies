package invin.com.similarmovies;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.commons.io.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * This is the home screen of the App
 *
 * @author Neil Pathare
 */
public class HomeScreenActivity extends Activity {

    //To show a loading indicator between Activities
    private ProgressBar spinner;

    public final static String TAG_ID;
    public final static String TAG_TITLE;
    public final static String TAG_MOVIES;

    public final static String INTENT_KEY;
    public final static String INTENT_MOVIE_ID;
    public final static String INTENT_MOVIE_NAME;
    public final static String INTENT_MOVIE_ID_NAME;

    static{
        TAG_ID = "id";
        TAG_TITLE = "title";
        TAG_MOVIES = "movies";

        INTENT_KEY = "com.invin.similarmovies.KEY";
        INTENT_MOVIE_ID = "com.invin.similarmovies.MOVIE_ID";
        INTENT_MOVIE_NAME = "com.invin.similarmovies.MOVIE_NAME";
        INTENT_MOVIE_ID_NAME = "com.invin.similarmovies.MOVIE_ID_NAME";
    }

    //'Movies' JSONArray
    private JSONArray moviesJSON;

    private String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
    private String apiMovieSearchQueryAppender = ".json?q=";
    private String apiMovieSearchConnector = "&page_limit=10&page=1&apikey=";

    private String errorMessage;
    private String apiKey;

    public HomeScreenActivity() {
        moviesJSON = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        //TODO: Look into build(); if it's still needed
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode
                    .ThreadPolicy
                    .Builder()
                    .permitAll()
                    .build();

            StrictMode.setThreadPolicy(policy);
        }

        //Default 'Up - Home' button functionality is intentionally disabled
        getActionBar().setDisplayHomeAsUpEnabled(false);

        //Create a loading spinner & leave it hidden until required
        spinner = (ProgressBar)findViewById(R.id.homeScreenProgressBar);
        spinner.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                openActionSettings();
                return true;
            case R.id.action_about:
                openActionAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when the user tries to find similar movies
     * This method will attempt to retrieve the ID of the entered movie & send that to an activity
     *  to search for & display similar movies.
     * If multiple movies are returned based on the movie name input, IDs & names will be sent to
     *  an activity for the user to select the movie he wants to search from this list.
     */
     public void searchAndSendMovieDetails(View view) {

         //Start the loading spinner as soon as we enter this method
         spinner.setVisibility(View.VISIBLE);
         //Hide the spinner when we are switching to another activity or otherwise leaving the method

         apiKey = returnRottenTomatoesAPIKeyFromAssets();

         if(apiKey.isEmpty()){
             Toast.makeText(
                     getApplicationContext(),
                     "An unexpected error occurred - API key is missing. Please re-install the App",
                     Toast.LENGTH_SHORT).show();
             spinner.setVisibility(View.GONE);
         }
         else{
             EditText editTextMovieName = (EditText) findViewById(R.id.editTextMovieName);
             String movieToSearchFor = editTextMovieName.getText().toString().replace(' ', '+');
             String movieSearchResultJSON;
             String movieSearchResultID;
             int numberOfMoviesFound;

             movieSearchResultJSON = returnSearchResultJSON(movieToSearchFor, apiKey);

             if (movieSearchResultJSON != null) {
                 try {
                     JSONObject movieSearchResultJSONObj = new JSONObject(movieSearchResultJSON);
                     JSONObject movieJSONObj;

                     // Get the "Movies" JSON array
                     moviesJSON = movieSearchResultJSONObj.getJSONArray(TAG_MOVIES);
                     numberOfMoviesFound = moviesJSON.length();

                     if (numberOfMoviesFound == 0){
                         editTextMovieName.setText("");
                         spinner.setVisibility(View.GONE);
                         Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
                         startActivity(intentToShowNoResults);
                     }
                     else if(numberOfMoviesFound == 1){
                         movieJSONObj = moviesJSON.getJSONObject(0);
                         movieSearchResultID = movieJSONObj.getString(TAG_ID);

                         editTextMovieName.setText("");
                         spinner.setVisibility(View.GONE);

                         Intent intentSendMovieID = new Intent(this, DisplaySimilarMoviesListActivity.class);
                         intentSendMovieID.putExtra(INTENT_MOVIE_ID, movieSearchResultID);
                         intentSendMovieID.putExtra(INTENT_KEY, apiKey);
                         startActivity(intentSendMovieID);
                     }
                     else{
                         HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<>();

                         for (int currentMovie = 0; currentMovie < numberOfMoviesFound; currentMovie++) {
                             movieJSONObj = moviesJSON.getJSONObject(currentMovie);

                             List<String> listOfIDsAndNames = new ArrayList<>();
                             listOfIDsAndNames.add(movieJSONObj.getString(TAG_ID));
                             listOfIDsAndNames.add(movieJSONObj.getString(TAG_TITLE));

                             movieIDNameHashCodeMap.put(
                                     movieJSONObj.getString(TAG_TITLE).hashCode(),
                                     listOfIDsAndNames
                             );
                         }

                         editTextMovieName.setText("");
                         spinner.setVisibility(View.GONE);

                         Intent intentSendMovieIDsAndNames = new Intent(this, DisplayMoviesForSelectionActivity.class);
                         intentSendMovieIDsAndNames.putExtra(INTENT_MOVIE_ID_NAME, movieIDNameHashCodeMap);
                         intentSendMovieIDsAndNames.putExtra(INTENT_KEY, apiKey);
                         startActivity(intentSendMovieIDsAndNames);
                     }
                 } catch (JSONException e) {
                     spinner.setVisibility(View.GONE);
                     editTextMovieName.setText("");
                     //TODO: Implement better error handling
                     errorMessage.concat(":Err:JSONException:");
                     //TODO: Implement logging
                 }

             } else {
                 spinner.setVisibility(View.GONE);
                 //TODO: Implement better error handling
                 errorMessage.concat(":Err:NoData:");
                 //TODO: Implement logging
                 editTextMovieName.setText("");
                 Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
                 startActivity(intentToShowNoResults);
             }
         }
     }

    /**
     * Makes the HTTP call to the API & returns the JSON response as a {@link java.lang.String}
     *
     * @param movieToSearchFor {@link java.lang.String} consisting of the movie name
     * @param apiKey {@link java.lang.String} consisting of the API key
     * @return {@link java.lang.String} consisting of the JSON response from the Rotten Tomatoes API
     */
    private String returnSearchResultJSON(String movieToSearchFor, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        StringBuilder apiURLBuilder = new StringBuilder();
        String apiResponse = "default";

        apiURLBuilder.append(apiURLPrepender);
        apiURLBuilder.append(apiMovieSearchQueryAppender);
        apiURLBuilder.append(movieToSearchFor);
        apiURLBuilder.append(apiMovieSearchConnector);
        apiURLBuilder.append(apiKey);

        HttpGet movieSearchHTTPRequest = new HttpGet(apiURLBuilder.toString());
        HttpResponse movieSearchHTTPResponse;

        InputStreamReader apiResponseInputStreamReader = null;
        BufferedReader inputStreamBufferedReader = null;
        InputStream inputStreamFromAPIResponse = null;

        try {
            movieSearchHTTPResponse = defaultHTTPClient.execute(movieSearchHTTPRequest);
            if(movieSearchHTTPResponse.equals(null)){
                apiResponse = ":Err:NullResponse:";
            }
            else{
                inputStreamFromAPIResponse = movieSearchHTTPResponse.getEntity().getContent();

                //TODO: Need a better way to check for Content Encoding
                if ("gzip".equals(movieSearchHTTPResponse.getEntity().getContentEncoding())){
                    inputStreamFromAPIResponse = new GZIPInputStream(inputStreamFromAPIResponse);
                }

                apiResponseInputStreamReader = new InputStreamReader(inputStreamFromAPIResponse);
                inputStreamBufferedReader = new BufferedReader(apiResponseInputStreamReader);

                StringBuilder inputStreamStringBuilder = new StringBuilder();
                String inputRead = inputStreamBufferedReader.readLine();

                while(inputRead != null) {
                    inputStreamStringBuilder.append(inputRead);
                    inputRead = inputStreamBufferedReader.readLine();
                }

                apiResponse = inputStreamStringBuilder.toString();
            }
        } catch (ClientProtocolException e) {
            //TODO: Implement better error handling
            errorMessage.concat(":Err:ClientProtocolException:"+e);
            //TODO: Implement logging
        } catch (IOException e) {
            //TODO: Implement better error handling
            errorMessage.concat(":Err:IOException:");
            //TODO: Implement logging
        }
        finally {
            IOUtils.closeQuietly(inputStreamFromAPIResponse);
            IOUtils.closeQuietly(apiResponseInputStreamReader);
            IOUtils.closeQuietly(inputStreamBufferedReader);
            return apiResponse;
        }
    }

    /**
     *  Returns the RottenTomatoes API key from the assets folder.
     *  ('Assets' is added through Android Studio, default location (/res/assets))
     *
     *  @return {@link java.lang.String} consisting of the RottenTomatoes API key
    * */
    private String returnRottenTomatoesAPIKeyFromAssets() {
        AssetManager assetManager = getAssets();
        BufferedReader bufferedAssetsFileReader = null;
        String propertiesFileName = "key.properties";
        String[] keyValue;

        StringBuilder keyBuilder = new StringBuilder();
        InputStream propertiesReader = null;
        try {
            propertiesReader = assetManager.open(propertiesFileName);
            bufferedAssetsFileReader = new BufferedReader(new InputStreamReader(propertiesReader, "UTF-8"));

            String str;
            while ((str=bufferedAssetsFileReader.readLine()) != null) {
                if(str.contains("RottenTomatoesAPIKey")){
                    keyValue = str.split("=");
                    keyBuilder.append(keyValue[1]);
                }
            }
        } catch (UnsupportedEncodingException e) {
            //TODO: Implement better error handling
            errorMessage.concat(":Err:UnsupportedEncodingException:"+e);
            //TODO: Implement logging
        } catch (IOException e) {
            //TODO: Implement better error handling
            errorMessage.concat(":Err:IOException:"+e);
            //TODO: Implement logging
        }
        finally {
            IOUtils.closeQuietly(propertiesReader);
            IOUtils.closeQuietly(bufferedAssetsFileReader);

            return keyBuilder.toString();
        }
    }

    /**
     * Handle the 'Settings' action from the Action Bar
     */
    public void openActionSettings(){
        Toast.makeText(
                getApplicationContext(),
                "Sorry, Settings are currently disabled",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle the 'About' action from the Action Bar
     */
    public void openActionAbout(){
//        Toast.makeText(
//                getApplicationContext(),
//                "Sorry, 'About' Currently Disabled",
//                Toast.LENGTH_SHORT).show();
          Intent intentToShowAboutActivity = new Intent(this, AboutActivity.class);
          startActivity(intentToShowAboutActivity);
    }

    /**
     * Load the Progress Bar Spinner
     *
     * @param view {@link android.view.View}
     */
    public void loadProgressSpinner(View view){
        spinner.setVisibility(View.VISIBLE);
    }
}