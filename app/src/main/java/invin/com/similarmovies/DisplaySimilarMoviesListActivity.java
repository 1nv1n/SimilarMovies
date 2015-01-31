package invin.com.similarmovies;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import invin.com.similarmovies.util.Constants;

/**
 * {@link android.app.ListActivity} that takes in an ID passed through the intent to use the API
 *  to search & display a list of similar movies.
 * If none were found, this activity ends itself & calls {@link invin.com.similarmovies.NoResultsActivity}
 */
public class DisplaySimilarMoviesListActivity extends ListActivity{

    //'Movies' JSONArray
    JSONArray moviesJSON;

    //Hashmap for the ListView
    private ArrayList<HashMap<String, String>> movieList;
    HashMap<Integer, List<String>> movieIDNameHashCodeMap;
    ArrayList<String> movieListArray;
    JSONObject movieJSONObj;

    //Track whether a list of movies was returned or not
    private boolean isMoviesListNull;
    //Verify that hash-coding served its purpose
    private boolean wasMovieMatched;

    private String apiKey;

    public DisplaySimilarMoviesListActivity() {
        movieJSONObj = null;
        moviesJSON = null;
        isMoviesListNull = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_similar_movies_list);

        /**
         * Finish the current activity & return to the previous open activity
         */
        Button returnButton = (Button)findViewById(R.id.returnButtonFromSimilarMoviesList);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode
                    .ThreadPolicy
                    .Builder()
                    .permitAll()
                    .build();

            StrictMode.setThreadPolicy(policy);
        }

        // Get the movie ID passed in
        Intent intent = getIntent();
        String movieID = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE_ID);
        String movieName = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE_NAME);

        apiKey = intent.getStringExtra(HomeScreenActivity.INTENT_KEY);
        String similarMoviesJSON = returnSimilarMoviesJSON(movieID.trim(), apiKey);

        if (similarMoviesJSON != null) {
            try {
                JSONObject similarMoviesJSONObj = new JSONObject(similarMoviesJSON);

                //Get the "Movies" JSON Array Node
                moviesJSON = similarMoviesJSONObj.getJSONArray(HomeScreenActivity.TAG_MOVIES);
                int numberOfMoviesFound = moviesJSON.length();

                if (numberOfMoviesFound == 0){
                    Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
                    startActivity(intentToShowNoResults);
                    finish();
                }
                else{
                    isMoviesListNull = false;
                    movieListArray = new ArrayList<String>();
                    movieIDNameHashCodeMap = new HashMap<>();

                    //Loop through all the movies in the JSON object
                    for (int i = 0; i < moviesJSON.length(); i++) {
                        movieJSONObj = moviesJSON.getJSONObject(i);

                        List<String> listOfIDsAndNames = new ArrayList<>();
                        listOfIDsAndNames.add(movieJSONObj.getString(HomeScreenActivity.TAG_ID));
                        listOfIDsAndNames.add(movieJSONObj.getString(HomeScreenActivity.TAG_TITLE));

                        movieIDNameHashCodeMap.put(
                                movieJSONObj.getString(HomeScreenActivity.TAG_TITLE).hashCode(),
                                listOfIDsAndNames
                        );

                        movieListArray.add(" "+movieJSONObj.getString(HomeScreenActivity.TAG_TITLE));
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOG, "JSONException"+e.toString());
                }
                Toast.makeText(
                        getApplicationContext(),
                        "An exception occurred. Please verify internet connectivity & restart the App",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "An exception occurred. Please verify internet connectivity & restart the App",
                    Toast.LENGTH_SHORT).show();
        }

        /**
         * If movieList is not null, we should update the parsed JSON data into the ListView
         */
        if(!(isMoviesListNull)){
            TextView similarMoviesTextView = (TextView) findViewById(R.id.similarMoviesTextView);
            similarMoviesTextView.setText("Movies similar to: "+movieName);
            similarMoviesTextView.setTypeface(null, Typeface.BOLD);

            ArrayAdapter<String> movieListAdapter = new ArrayAdapter <String>(
                    DisplaySimilarMoviesListActivity.this,
                    R.layout.movies_list,
                    R.id.movieTitle,
                    movieListArray);

            setListAdapter(movieListAdapter);
        }
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        //Get the selected movie
        String selectedItem = (String) getListView().getItemAtPosition(position);
        //Alternative method to get the selected movie:
        //String selectedItem = (String) getListAdapter().getItem(position);

        //TODO: This is a bad design pattern, need to implement something better.
        //Remove the extra blank space that we had added earlier (for padding purposes)
        selectedItem = selectedItem.substring(1);

        /**
         * In order to send the correct movie ID to the next activity,
         *  we need to match the hash code of the selected movie with ones from the list
         *  sent from the previous activity
         */
        for (HashMap.Entry<Integer, List<String>> hashMovieEntry : movieIDNameHashCodeMap.entrySet()) {
            if(selectedItem.hashCode() == hashMovieEntry.getKey()){
                wasMovieMatched = true;
                List<String> listOfIDsAndNames = hashMovieEntry.getValue();

                Intent intentSendMovieNameAndID = new Intent(this, DisplaySimilarMoviesListActivity.class);
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_MOVIE_NAME, selectedItem);
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_MOVIE_ID, listOfIDsAndNames.get(0));
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_KEY, apiKey);

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOG, "Selected Movie:"+selectedItem);
                    Log.d(Constants.LOG, "ID:"+listOfIDsAndNames.get(0));
                }

                startActivity(intentSendMovieNameAndID);
            }
        }

        if(!wasMovieMatched){
            Toast.makeText(
                    getApplicationContext(),
                    "Something went wrong, please restart the App",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_similar_movies, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_about:
                openActionAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String returnSimilarMoviesJSON(String movieID, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
        String apiURLSimilarConnector = "similar.json?apikey=";
        String forwardSlash = "/";
        String apiResponse = "default";

        StringBuilder apiURLBuilder = new StringBuilder();
        apiURLBuilder.append(apiURLPrepender);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(movieID);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(apiURLSimilarConnector);
        apiURLBuilder.append(apiKey);

        HttpGet similarMoviesHTTPRequest = new HttpGet(apiURLBuilder.toString());
        HttpResponse similarMoviesHTTPResponse;

        InputStream inputStreamFromAPIResponse = null;
        BufferedReader inputStreamBufferedReader = null;
        InputStreamReader apiResponseInputStreamReader = null;

        try {
            similarMoviesHTTPResponse = defaultHTTPClient.execute(similarMoviesHTTPRequest);
            if(similarMoviesHTTPResponse.equals(null)){
                apiResponse = ":Err:NullResponse:";
            }
            else{
                inputStreamFromAPIResponse = similarMoviesHTTPResponse.getEntity().getContent();
                if ("gzip".equals(similarMoviesHTTPResponse.getEntity().getContentEncoding())){
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
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "ClientProtocolException"+e.toString());
            }
            Toast.makeText(
                    getApplicationContext(),
                    "An exception occurred. Please verify internet connectivity & restart the App",
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "IOException"+e.toString());
            }
            Toast.makeText(
                    getApplicationContext(),
                    "An exception occurred. Please verify internet connectivity & restart the App",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "Exception"+e.toString());
            }
            Toast.makeText(
                    getApplicationContext(),
                    "An exception occurred. Please verify internet connectivity & restart the App",
                    Toast.LENGTH_SHORT).show();
        }
        finally {
            IOUtils.closeQuietly(inputStreamBufferedReader);
            IOUtils.closeQuietly(inputStreamFromAPIResponse);
            IOUtils.closeQuietly(apiResponseInputStreamReader);

            return apiResponse;
        }
    }

    //TODO: Externalize this method into a Util package
    /**
     * Handle the 'Settings' action from the Action Bar
     */
    public void openActionSettings(){
        Toast.makeText(
                getApplicationContext(),
                "Sorry, Settings are currently disabled",
                Toast.LENGTH_SHORT).show();
    }

    //TODO: Externalize this method into a Util package
    /**
     * Handle the 'About' action from the Action Bar
     */
    public void openActionAbout(){
        Intent intentToShowAboutActivity = new Intent(this, AboutActivity.class);
        startActivity(intentToShowAboutActivity);
    }
}