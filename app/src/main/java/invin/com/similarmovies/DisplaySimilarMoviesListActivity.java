package invin.com.similarmovies;

import android.content.Intent;
import android.os.Bundle;
import android.app.ListActivity;
import android.os.Build;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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
 * ListActivity to display a list of movies similar to the movie
 * passed in from the Home Screen
 */
public class DisplaySimilarMoviesListActivity extends ListActivity{

    private static final String TAG_MOVIES = "movies";
    private static final String TAG_ID = "id";
    private static final String TAG_TITLE = "title";

    //Movies JSONArray
    JSONArray moviesJSON = null;

    //Hashmap for ListView
    ArrayList<HashMap<String, String>> movieList;

    private String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
    private String apiSimilarConnector = "similar.json?apikey=";

    private String errorMessage = "";
    private String forwardSlash = "/";

    private int numberOfMoviesFound = 0;

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

        movieList = new ArrayList<HashMap<String, String>>();

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
        String movieSearchResultID = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE);
        String apiKey = intent.getStringExtra(HomeScreenActivity.INTENT_KEY);

        String similarMoviesJSON = returnSimilarMoviesJSON(movieSearchResultID.trim(), apiKey);
        //apiKey = returnRottenTomatoesAPIKeyFromAssets();

        if (similarMoviesJSON != null) {
            try {
                JSONObject similarMoviesJSONObj = new JSONObject(similarMoviesJSON);

                //Get the "Movies" JSON Array Node
                moviesJSON = similarMoviesJSONObj.getJSONArray(TAG_MOVIES);
                numberOfMoviesFound = moviesJSON.length();

                if (numberOfMoviesFound == 0){
                    errorMessage.concat(":Err:NoSimilarMovie:");
                    Intent intentToShowNoResults = new Intent(this, NoResultsActivity.class);
                    startActivity(intentToShowNoResults);
                }
                else{
                    //Loop through all the movies in the JSON object
                    for (int i = 0; i < moviesJSON.length(); i++) {
                        JSONObject movieJSONObj = moviesJSON.getJSONObject(i);

                        String movieID = movieJSONObj.getString(TAG_ID);
                        String movieTitle = movieJSONObj.getString(TAG_TITLE);

                        //Temp Hash Map for a single movie
                        HashMap<String, String> movieHashMap = new HashMap<String, String>();

                        //Adding each child node to the Hash Map (Key => Value)
                        movieHashMap.put(TAG_ID, movieID);
                        movieHashMap.put(TAG_TITLE, movieTitle);

                        //Adding the movie to the movie list
                        movieList.add(movieHashMap);
                    }
                }
            } catch (JSONException e) {
                //TODO: Implement better error handling
                errorMessage.concat(":Err:JSONException:");
            }
        } else {
            errorMessage.concat(":Err:NoData:");
        }

        /**
         * Updating the parsed JSON data into our ListView
         * */
        ListAdapter adapter = new SimpleAdapter(
                DisplaySimilarMoviesListActivity.this,
                movieList,
                R.layout.list_item,
                new String[] { TAG_TITLE },
                new int[] { R.id.movie_title });

        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_message, menu);
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

    private String returnSimilarMoviesJSON(String movieID, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        StringBuilder apiURLBuilder = new StringBuilder();
        apiURLBuilder.append(apiURLPrepender);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(movieID);
        apiURLBuilder.append(forwardSlash);
        apiURLBuilder.append(apiSimilarConnector);
        apiURLBuilder.append(apiKey);

        String apiResponse = "";

        InputStream inputStreamFromAPIResponse = null;

        HttpGet similarMoviesHTTPRequest = new HttpGet(apiURLBuilder.toString());

        HttpResponse similarMoviesHTTPResponse;
        try {
            similarMoviesHTTPResponse = defaultHTTPClient.execute(similarMoviesHTTPRequest);
            if(similarMoviesHTTPResponse.equals(null)){
                apiResponse = ":Err:NullResponse:";
            }
            else{
                inputStreamFromAPIResponse = similarMoviesHTTPResponse.getEntity().getContent();
                if ("gzip".equals(similarMoviesHTTPResponse.getEntity().getContentEncoding().toString())){
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