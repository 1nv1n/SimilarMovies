package invin.com.similarmovies;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
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

    //ProcessDialog while the list is generated
    private ProgressDialog progressDialog;

    //'Movies' JSONObject & JSONArray
    JSONArray moviesJSON;
    JSONObject movieJSONObj;

    //HashMap for the movie ID, name * hash-code of the name
    HashMap<Integer, List<String>> movieIDNameHashCodeMap;

    //ArrayList for storing the movie names to display
    ArrayList<String> movieListArray;

    //Track whether a list of movies was returned or not
    private boolean isMoviesListNull;
    //Verify that hash-coding served its purpose
    private boolean wasMovieMatched;

    private String apiKey;
    private String movieID;
    private String movieName;

    private int mProgressStatus = 0;

    public DisplaySimilarMoviesListActivity() {
        movieJSONObj = null;
        moviesJSON = null;
        isMoviesListNull = true;
        wasMovieMatched = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_similar_movies_list);

        // Creating a progress dialog window
        progressDialog = new ProgressDialog(this);
        // Close the dialog window on pressing back button
        progressDialog.setCancelable(true);
        // Set it as a spinner
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        /** Setting a message for this progress dialog
         * Use the method setTitle(), for setting a title
         * for the dialog window
         */
        progressDialog.setMessage("Processing...");

        /**
         * Finish the current activity & return to the previous open activity
         */
        Button returnButton = (Button)findViewById(R.id.returnButtonFromSimilarMoviesList);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // Get the movie ID passed in
        Intent intent = getIntent();
        apiKey = intent.getStringExtra(HomeScreenActivity.INTENT_KEY);
        movieID = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE_ID);
        movieName = intent.getStringExtra(HomeScreenActivity.INTENT_MOVIE_NAME);

        //Start a new AsyncTaskRunner to ensure that we always process on a new thread
        new AsyncTaskRunner().execute(apiKey, movieID, movieName);
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        //Get the selected movie
        String selectedItem = (String) getListView().getItemAtPosition(position);
        //Alternative method to get the selected movie:
        //String selectedItem = (String) getListAdapter().getItem(position);

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

    /**
     * Returns a {@link java.lang.String} containing the JSON consisting of similar movies returned
     *  from the API based n the movie ID sent in
     *
     * @param movieID ({@link java.lang.String} consisting of the RottenTomatoes's ID of the movie)
     * @param apiKey ({@link java.lang.String} consisting of the RottenTomatoes API Key)
     * @return apiResponse ({@link java.lang.String} containing the API's response as a JSON)
     */
    private String returnSimilarMoviesJSON(String movieID, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
        String apiURLSimilarConnector = "similar.json?apikey=";
        String forwardSlash = "/";
        String apiResponse = "defaultAPIResponse";

        HttpGet similarMoviesHTTPRequest = new HttpGet(
                apiURLPrepender +
                forwardSlash +
                movieID +
                forwardSlash +
                apiURLSimilarConnector +
                apiKey);

        HttpResponse similarMoviesHTTPResponse;

        InputStream inputStreamFromAPIResponse = null;
        BufferedReader inputStreamBufferedReader = null;
        InputStreamReader apiResponseInputStreamReader = null;

        try {
            similarMoviesHTTPResponse = defaultHTTPClient.execute(similarMoviesHTTPRequest);
            //As long as we don't get a null response, continue processing
            if(!similarMoviesHTTPResponse.equals(null)){
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
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "IOException"+e.toString());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "Exception"+e.toString());
            }
        }
        finally {
            IOUtils.closeQuietly(inputStreamBufferedReader);
            IOUtils.closeQuietly(inputStreamFromAPIResponse);
            IOUtils.closeQuietly(apiResponseInputStreamReader);
            return apiResponse;
        }
    }

    /**
     * Handle the 'About' action from the Action Bar
     */
    public void openActionAbout(){
        Intent intentToShowAboutActivity = new Intent(this, AboutActivity.class);
        startActivity(intentToShowAboutActivity);
    }

    /**
     * This should handle all the processing & take it off the main thread
     *
     * @see android.os.AsyncTask
     */
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private final String noMoviesFound = "noMoviesFound";
        private final String errorOccurred = "errorOccurred";
        private final String endOfProcessing = "endOfProcessing";
        private final String multipleResultsFound = "multipleResultsFound";

        /**
         * This method will attempt to retrieve a list of movies similar to the ID of the movie
         *  received.
         *
         * @see android.os.AsyncTask#doInBackground(Object[])
         */
        @Override
        protected String doInBackground(String... params) {
            // Calls onProgressUpdate()
            publishProgress("Processing...");

            // Get the JSON of similar movies
            String similarMoviesJSON = returnSimilarMoviesJSON(movieID.trim(), apiKey);

            if (null != similarMoviesJSON) {
                if(!similarMoviesJSON.equals("defaultAPIResponse")){
                    try {
                        JSONObject similarMoviesJSONObj = new JSONObject(similarMoviesJSON);
                        moviesJSON = similarMoviesJSONObj.getJSONArray(HomeScreenActivity.TAG_MOVIES);
                        int numberOfMoviesFound = moviesJSON.length();

                        if (numberOfMoviesFound == 0){
                            isMoviesListNull = true;
                            return noMoviesFound;
                        }
                        else{
                            isMoviesListNull = false;
                            movieListArray = new ArrayList<>();
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

                                movieListArray.add(movieJSONObj.getString(HomeScreenActivity.TAG_TITLE));
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(Constants.LOG, "JSONException"+e.toString());
                        }
                        return errorOccurred;
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(Constants.LOG, "Exception"+e.toString());
                        }
                        return errorOccurred;
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, "Default API response returned for ID:" + movieID);
                    }
                    return errorOccurred;
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOG, "API Returned 'null' for ID:" + movieID);
                }
                return errorOccurred;
            }

            return endOfProcessing;
        }

        /**
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String backgroundProcessingResult) {
            //progressDialog.setProgress(mProgressStatus);

            Intent intentToShowNoResults = new Intent(DisplaySimilarMoviesListActivity.this, NoResultsActivity.class);

            switch (backgroundProcessingResult){
                case noMoviesFound:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, noMoviesFound);
                    }
                    startActivity(intentToShowNoResults);
                    finish();
                    break;

                case errorOccurred:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, errorOccurred);
                    }
                    Toast.makeText(
                            getApplicationContext(),
                            "An exception occurred. Please verify internet connectivity & restart the App",
                            Toast.LENGTH_SHORT).show();
                    break;

                case endOfProcessing:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, endOfProcessing);
                    }
                    break;

                default:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, "onPostExecute:default");
                    }
                    startActivity(intentToShowNoResults);
                    break;
            }

            /**
             * If movieList is not null, we should update the parsed JSON data into the ListView
             */
            if(!(isMoviesListNull)){
                TextView similarMoviesTextView = (TextView) findViewById(R.id.similarMoviesTextView);
                similarMoviesTextView.setText("Movies similar to: "+movieName);
                similarMoviesTextView.setTypeface(null, Typeface.BOLD);

                ArrayAdapter<String> movieListAdapter = new ArrayAdapter<>(
                        DisplaySimilarMoviesListActivity.this,
                        R.layout.movies_list,
                        R.id.movieTitle,
                        movieListArray);

                setListAdapter(movieListAdapter);
            }
            progressDialog.dismiss();
        }

        /**
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            /**
             * Things to be done before execution of long running operation.
             */
            progressDialog.show();
        }

        /**
         * @see android.os.AsyncTask#onProgressUpdate(Object[])
         */
        @Override
        protected void onProgressUpdate(String... text) {
            /**
             * Things to be done while execution of long running operation is in progress.
             * ProgessDialog can be used here
             */
            //progressDialog.setProgress(mProgressStatus);
        }
    }
}