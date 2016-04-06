package invin.com.similarmovies.activity;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import invin.com.similarmovies.BuildConfig;
import invin.com.similarmovies.R;
import invin.com.similarmovies.util.Constants;
import invin.com.similarmovies.util.HTTPUtil;

/**
 * {@link android.app.ListActivity} that takes in an ID passed through the intent to use the API
 *  to search & display a list of similar movies.
 * If none were found, this activity ends itself & calls {@link NoResultsActivity}
 */
public class DisplaySimilarMoviesActivity extends ListActivity{

    // ProcessDialog while the list is generated
    private ProgressDialog progressDialog;

    // 'Movies' JSONObject & JSONArray
    private JSONArray moviesJSON;
    private JSONObject movieJSONObj;

    // HashMap for the movie ID, name * hash-code of the name
    private HashMap<Integer, List<String>> movieIDNameHashCodeMap;

    // ArrayList for storing the movie names to display
    private ArrayList<String> movieListArray;

    // Track whether a list of movies was returned or not
    private boolean isMoviesListNull;

    // Verify that hash-coding served its purpose
    private boolean wasMovieMatched;

    // Strings passed in from the previous activities
    private String apiKey;
    private String movieID;
    private String movieName;

    private int mProgressStatus = 0;

    /**
     * Initializes member variables
     */
    public DisplaySimilarMoviesActivity() {
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
        apiKey = intent.getStringExtra(Constants.INTENT_KEY);
        movieID = intent.getStringExtra(Constants.INTENT_MOVIE_ID);
        movieName = intent.getStringExtra(Constants.INTENT_MOVIE_NAME);

        // Start a new AsyncTaskRunner to ensure that we always process on a new thread
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

                Intent intentSendMovieNameAndID = new Intent(this, DisplaySimilarMoviesActivity.class);
                intentSendMovieNameAndID.putExtra(Constants.INTENT_MOVIE_NAME, selectedItem);
                intentSendMovieNameAndID.putExtra(Constants.INTENT_MOVIE_ID, listOfIDsAndNames.get(0));
                intentSendMovieNameAndID.putExtra(Constants.INTENT_KEY, apiKey);

                if (BuildConfig.DEBUG) {
                    Log.d(DisplayMoviesForSelectionActivity.class.getCanonicalName(), "Selected Movie:" + selectedItem); //$NON-NLS-1$
                    Log.d(DisplayMoviesForSelectionActivity.class.getCanonicalName(), "Selected Movie ID:" + listOfIDsAndNames.get(0)); //$NON-NLS-1$
                }

                startActivity(intentSendMovieNameAndID);
            }
        }

        if(!wasMovieMatched){
            Toast.makeText(getApplicationContext(), R.string.text_error_restart, Toast.LENGTH_SHORT).show();
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
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(), R.string.text_settings_disabled, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            String similarMoviesJSON = HTTPUtil.returnSearchResultOrSimilarMoviesJSON(Constants.SEARCH_BY_MOVIE_ID, movieID.trim(), apiKey);
            if (null != similarMoviesJSON) {
                if(!similarMoviesJSON.equals("defaultAPIResponse")){ //$NON-NLS-1$
                    try {
                        JSONObject similarMoviesJSONObj = new JSONObject(similarMoviesJSON);
                        Log.i("DEBUG", similarMoviesJSONObj.toString());
                        moviesJSON = similarMoviesJSONObj.getJSONArray(Constants.TAG_MOVIES);

                        if (moviesJSON.length() == 0){
                            isMoviesListNull = true;
                            return noMoviesFound;
                        } else {
                            isMoviesListNull = false;
                            movieListArray = new ArrayList<>();
                            movieIDNameHashCodeMap = new HashMap<>();

                            // Loop through all the movies in the JSON object
                            for (int i = 0; i < moviesJSON.length(); i++) {
                                movieJSONObj = moviesJSON.getJSONObject(i);

                                List<String> movieIDTitleList = new ArrayList<>();
                                movieIDTitleList.add(movieJSONObj.getString(Constants.TAG_ID));
                                movieIDTitleList.add(movieJSONObj.getString(Constants.TAG_TITLE));

                                movieIDNameHashCodeMap.put(
                                        movieJSONObj.getString(Constants.TAG_TITLE).hashCode(),
                                        movieIDTitleList
                                );

                                movieListArray.add(movieJSONObj.getString(Constants.TAG_TITLE));
                            }
                        }
                    } catch (JSONException jsonException) {
                        Log.e(DisplaySimilarMoviesActivity.class.getCanonicalName(), jsonException.getLocalizedMessage()); //$NON-NLS-1$
                        return errorOccurred;
                    } catch (Exception exception) {
                        Log.e(DisplaySimilarMoviesActivity.class.getCanonicalName(), exception.getLocalizedMessage()); //$NON-NLS-1$
                        return errorOccurred;
                    }
                } else {
                    Log.d(DisplaySimilarMoviesActivity.class.getCanonicalName(), "Default API response returned for ID:" + movieID); //$NON-NLS-1$
                    return errorOccurred;
                }
            } else {
                Log.e(DisplaySimilarMoviesActivity.class.getCanonicalName(), "API Returned 'null' for ID:" + movieID); //$NON-NLS-1$
                return errorOccurred;
            }

            return endOfProcessing;
        }

        /**
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String backgroundProcessingResult) {
            progressDialog.setProgress(mProgressStatus);
            Intent intentToShowNoResults = new Intent(DisplaySimilarMoviesActivity.this, NoResultsActivity.class);

            switch (backgroundProcessingResult){
                case noMoviesFound:
                    Log.d(DisplaySimilarMoviesActivity.class.getCanonicalName(), noMoviesFound);
                    startActivity(intentToShowNoResults);
                    finish();
                    break;

                case errorOccurred:
                    Log.d(DisplaySimilarMoviesActivity.class.getCanonicalName(), errorOccurred);
                    Toast.makeText(getApplicationContext(), R.string.text_error_unexpected, Toast.LENGTH_SHORT).show();
                    break;

                case endOfProcessing:
                    Log.d(DisplaySimilarMoviesActivity.class.getCanonicalName(), endOfProcessing);
                    break;

                default:
                    Log.d(DisplaySimilarMoviesActivity.class.getCanonicalName(), "onPostExecute:default"); //$NON-NLS-1$
                    startActivity(intentToShowNoResults);
                    break;
            }

            /**
             * If movieList is not null, we should update the parsed JSON data into the ListView
             */
            if(!(isMoviesListNull)){
                ArrayAdapter<String> movieListAdapter = new ArrayAdapter<>(
                        DisplaySimilarMoviesActivity.this,
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
            progressDialog.setProgress(mProgressStatus);
        }
    }
}