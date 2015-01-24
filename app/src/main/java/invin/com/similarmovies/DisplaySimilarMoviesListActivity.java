package invin.com.similarmovies;

import android.content.Intent;
import android.app.ListActivity;

import android.os.Bundle;
import android.os.Build;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import java.util.zip.GZIPInputStream;

/**
 * {@link android.app.ListActivity} that takes in an ID passed through the intent to use the API
 *  to search & display a list of similar movies.
 * If none were found, this activity ends itself & calls {@link invin.com.similarmovies.NoResultsActivity}
 *
 * @author Neil Pathare
 */
public class DisplaySimilarMoviesListActivity extends ListActivity{

    //'Movies' JSONArray
    JSONArray moviesJSON = null;

    //Hashmap for the ListView
    ArrayList<HashMap<String, String>> movieList;

    //String URL helpers
    private final String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
    private final String apiSimilarConnector = "similar.json?apikey=";

    private final String errorMessage = "";
    private final String forwardSlash = "/";

    private boolean isMoviesListNull = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_similar_movies_list);

        Button returnButton = (Button)findViewById(R.id.returnButtonFromSimilarMoviesList);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        ListView lv = getListView();

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode
                    .ThreadPolicy
                    .Builder()
                    .permitAll()
                    .build();

            StrictMode.setThreadPolicy(policy);
        }

        // Get the movie ID passed in from the Home Screen
        Intent intent = getIntent();
        String movieSearchResultID = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE_ID);
        String apiKey = intent.getStringExtra(HomeScreenActivity.INTENT_KEY);

        String similarMoviesJSON = returnSimilarMoviesJSON(movieSearchResultID.trim(), apiKey);

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
                    movieList = new ArrayList<HashMap<String, String>>();

                    //Loop through all the movies in the JSON object
                    for (int i = 0; i < moviesJSON.length(); i++) {
                        JSONObject movieJSONObj = moviesJSON.getJSONObject(i);

                        String movieID = movieJSONObj.getString(HomeScreenActivity.TAG_ID);
                        String movieTitle = movieJSONObj.getString(HomeScreenActivity.TAG_TITLE);

                        //Temp Hash Map for a single movie
                        HashMap<String, String> movieHashMap = new HashMap<String, String>();

                        //Adding each child node to the Hash Map (Key => Value)
                        movieHashMap.put(HomeScreenActivity.TAG_ID, movieID);
                        movieHashMap.put(HomeScreenActivity.TAG_TITLE, movieTitle);

                        //Adding the movie to the movie list
                        movieList.add(movieHashMap);
                    }
                    isMoviesListNull = false;
                }
            } catch (JSONException e) {
                //TODO: Implement better error handling
                errorMessage.concat(":Err:JSONException:");
                //TODO: Implement logging
            }
        } else {
            //TODO: Implement better error handling
            errorMessage.concat(":Err:NoData:");
            //TODO: Implement logging
        }

        /**
         * If movieList is not null, we should update the parsed JSON data into the ListView
         */
        if(!(isMoviesListNull)){
            ListAdapter adapter = new SimpleAdapter(
                    DisplaySimilarMoviesListActivity.this,
                    movieList,
                    R.layout.movies_list,
                    new String[] { HomeScreenActivity.TAG_TITLE },
                    new int[] { R.id.movieTitle });
            setListAdapter(adapter);
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
        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                openActionSettings();
//                return true;
            case R.id.action_about:
                openActionAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String returnSimilarMoviesJSON(String movieID, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        String apiResponse = "default";
        StringBuilder apiURLBuilder = new StringBuilder();
        apiURLBuilder.append(apiURLPrepender);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(movieID);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(apiSimilarConnector);
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
            errorMessage.concat(":Err:ClientProtocolException:");
            System.out.println(":Err:ClientProtocolException:");
        } catch (IOException e) {
            errorMessage.concat(":Err:IOException:");
            System.out.println(":Err:IOException:");
        } catch (Exception e) {
            errorMessage.concat(":Err:Exception:");
            System.out.println(":Err:Exception:");
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