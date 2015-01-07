package invin.com.similarmovies;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

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
 * Activity to display the home screen of the App
 */
public class HomeScreenActivity extends Activity {

    private static final String TAG_MOVIES = "movies";
    private static final String TAG_ID = "id";

    public final static String EXTRA_MESSAGE;
    static{
        EXTRA_MESSAGE = "com.invin.similarmovies.MESSAGE";
    }

    //Movies JSONArray
    private JSONArray moviesJSON = null;

    private ArrayList<HashMap<String, String>> movieSearchResult;

    private String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
    private String apiMovieSearchQueryAppender = ".json?q=";
    private String apiMovieSearchConnector = "&page_limit=10&page=1&apikey=";
    private String apiKey = "TODO:ReplaceThisKeyWithExternalResource";

    private String errorMessage = "";
    private String forwardSlash = "/";
    private String movieToSearchFor = "";

    private int numberOfMoviesFound = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode
                    .ThreadPolicy
                    .Builder()
                    .permitAll()
                    .build();

            StrictMode.setThreadPolicy(policy);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user clicks the 'Search' button
     * This will retrieve the movie ID of the entered movie & send that to the next activity
     * */
    public void sendMovieID(View view) {
        EditText editTextMovieName = (EditText) findViewById(R.id.editTextMovieName);
        String movieSearchResultJSON = "";
        String movieSearchResultID = "id";

        movieSearchResult =  new ArrayList<HashMap<String, String>>();
        movieToSearchFor = editTextMovieName.getText().toString();
        movieToSearchFor = movieToSearchFor.replace(' ', '+');

        movieSearchResultJSON = returnSearchResultJSON(movieToSearchFor);

        if (movieSearchResultJSON != null) {
            try {
                JSONObject movieSearchResultJSONObj = new JSONObject(movieSearchResultJSON);

                // Get the "Movies" JSON Array Node
                moviesJSON = movieSearchResultJSONObj.getJSONArray(TAG_MOVIES);
                numberOfMoviesFound = moviesJSON.length();

                if (numberOfMoviesFound == 0){
                    errorMessage.concat(":Err:NoSuchMovie:");
                    movieSearchResultID = "0";
                    Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
                    startActivity(intentToShowNoResults);
                }
                else if(numberOfMoviesFound == 1){
                    JSONObject movieJSONObj = moviesJSON.getJSONObject(1);
                    movieSearchResultID = movieJSONObj.getString(TAG_ID);
                    Intent intentSendMovieID = new Intent(this, DisplaySimilarMoviesListActivity.class);
                    intentSendMovieID.putExtra(EXTRA_MESSAGE, movieSearchResultID);
                    startActivity(intentSendMovieID);
                }
                else{
                    ArrayList<String> movieSearchResultIDs = new ArrayList<>();
                    for (int iterateThroughMoviesFound = 0; iterateThroughMoviesFound < numberOfMoviesFound; iterateThroughMoviesFound++) {
                        JSONObject movieJSONObj = moviesJSON.getJSONObject(iterateThroughMoviesFound);
                        movieSearchResultIDs.add(movieJSONObj.getString(TAG_ID));
                    }
                    Intent intentSendMovieIDs = new Intent(this, DisplayMoviesForSelectionActivity.class);
                    Bundle movieIDBundle = new Bundle();
                    String[] movieSearchResultIDStringArray = movieSearchResultIDs.toArray(new String[movieSearchResultIDs.size()]);
                    movieIDBundle.putStringArray(EXTRA_MESSAGE, movieSearchResultIDStringArray);
                    intentSendMovieIDs.putExtras(movieIDBundle);
                    startActivity(intentSendMovieIDs);
                }
            } catch (JSONException e) {
                //TODO: Implement better error handling
                errorMessage.concat(":Err:JSONException:");
            }
        } else {
            errorMessage.concat(":Err:NoData:");
            Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
            startActivity(intentToShowNoResults);
        }
    }

    private String returnSearchResultJSON(String movieToSearchFor) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        StringBuilder apiURLBuilder = new StringBuilder();
        apiURLBuilder.append(apiURLPrepender);
        apiURLBuilder.append(apiMovieSearchQueryAppender);
        apiURLBuilder.append(movieToSearchFor);
        apiURLBuilder.append(apiMovieSearchConnector);
        apiURLBuilder.append(apiKey);

        String apiURL = apiURLBuilder.toString();
        String apiResponse = "";

        InputStream inputStreamFromAPIResponse = null;

        HttpGet movieSearchHTTPRequest = new HttpGet(apiURL);

        HttpResponse movieSearchHTTPResponse;
        try {
            movieSearchHTTPResponse = defaultHTTPClient.execute(movieSearchHTTPRequest);
            if(movieSearchHTTPResponse.equals(null)){
                apiResponse = ":Err:NullResponse:";
            }
            else{
                inputStreamFromAPIResponse = movieSearchHTTPResponse.getEntity().getContent();
                if ("gzip".equals(movieSearchHTTPResponse.getEntity().getContentEncoding())){
                    inputStreamFromAPIResponse = new GZIPInputStream(inputStreamFromAPIResponse);
                }
                InputStreamReader apiResponseInputStreamReader = new InputStreamReader(inputStreamFromAPIResponse);
                StringBuilder inputStreamStringBuilder = new StringBuilder();
                BufferedReader inputStreamBufferedReader = new BufferedReader(apiResponseInputStreamReader);
                String inputRead = inputStreamBufferedReader.readLine();

                while(inputRead != null) {
                    inputStreamStringBuilder.append(inputRead);
                    inputRead = inputStreamBufferedReader.readLine();
                }

                apiResponse = inputStreamStringBuilder.toString();
            }
        } catch (ClientProtocolException e) {
            errorMessage.concat(":Err:ClientProtocolException:");
        } catch (IOException e) {
            errorMessage.concat(":Err:IOException:");
        }
        finally {
            return apiResponse;
        }
    }
}