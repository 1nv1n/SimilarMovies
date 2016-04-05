package invin.com.similarmovies.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import invin.com.similarmovies.R;
import invin.com.similarmovies.util.Constants;
import invin.com.similarmovies.util.HTTPUtil;
import invin.com.similarmovies.util.PropertyUtil;

import static invin.com.similarmovies.R.menu.menu_main;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Home Screen of the App.
 *  Presents a text-entry method and allows search initiation.
 *
 * @author Neil Pathare
 */
public class SearchScreenActivity extends Activity {

    // To show a loading indicator between Activities whilst data processing is ongoing
    private ProgressBar spinner;

    // 'Movies' JSONArray
    private JSONArray moviesJSONArray;

    // Text that get the movie name from the user
    private EditText editTextMovieName;

    //Basic constructor
    public SearchScreenActivity() {
        moviesJSONArray = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        // Default 'Up - Home' button functionality is intentionally disabled
        try {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        } catch (NullPointerException nullPointerException) {
            Log.e(SearchScreenActivity.class.getCanonicalName(), nullPointerException.getLocalizedMessage());
        }

        // Create a loading spinner & leave it hidden until required
        spinner = (ProgressBar)findViewById(R.id.homeScreenProgressBar);
        spinner.setVisibility(View.GONE);

        /**
         * Identify the EditText & set an Action Listener to automatically pass the contents
         * to the relevant method while disabling the 'Enter' key
         */
        editTextMovieName = (EditText) findViewById(R.id.editTextMovieName);
        editTextMovieName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handledIMEAction = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if(isAValidMovieName(editTextMovieName.getText().toString())){
                        // Start a new AsyncTaskRunner to ensure that we always process on a new thread
                        new AsyncTaskRunner().execute(editTextMovieName.getText().toString());
                        handledIMEAction = true;
                    }
                }
                return handledIMEAction;
            }
        });

        Button searchMovieButton = (Button) findViewById(R.id.searchMovieButton);
        searchMovieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAValidMovieName(editTextMovieName.getText().toString())) {
                    // Start a new AsyncTaskRunner to ensure that we always process on a new thread
                    new AsyncTaskRunner().execute(editTextMovieName.getText().toString());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * Handle action bar item clicks here. The action bar will
         * automatically handle clicks on the Home/Up button, so long
         * as you specify a parent activity in AndroidManifest.xml.
        */
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
     * Verify that the entered movie name follows guidelines
     *
     * @param enteredMovieName 9{@link java.lang.String} containing the movie name)
     * @return TRUE if the entered text is valid, FALSE otherwise
     */
    private boolean isAValidMovieName(String enteredMovieName){
        int MAXIMUM_MOVIE_NAME_LENGTH = 225;
        boolean reset = false;

        if(isBlank(enteredMovieName)) {
            Toast.makeText(getApplicationContext(), R.string.text_no_name, Toast.LENGTH_SHORT).show();
            reset = true;
        }
        else if (!enteredMovieName.matches("[a-zA-Z0-9.?-_: ]*")) {
            Toast.makeText(getApplicationContext(), R.string.text_reenter, Toast.LENGTH_SHORT).show();

            reset = true;
        }
        else if (enteredMovieName.length() > MAXIMUM_MOVIE_NAME_LENGTH) {
            Toast.makeText(getApplicationContext(), R.string.text_long_name, Toast.LENGTH_SHORT).show();
            reset = true;
         }

        if(reset) {
            // Reset the EditText & hide the loading spinner
            editTextMovieName.setText("");
            spinner.setVisibility(View.GONE);

            return false;
        }

        // Return true if everything is in order
        return true;
    }

    /**
     * This should handle all the processing & take it off the main thread
     *
     * @see android.os.AsyncTask
     */
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String enteredName;

        private final String noMoviesFound = "noMoviesFound"; //$NON-NLS-1$
        private final String oneResultFound = "oneResultFound"; //$NON-NLS-1$
        private final String endOfProcessing = "endOfProcessing"; //$NON-NLS-1$
        private final String multipleResultsFound = "multipleResultsFound"; //$NON-NLS-1$

        /**
         * This method will attempt to retrieve the ID of the entered movie & send that to
         *  {@link DisplaySimilarMoviesActivity} to search for & display
         *  similar movies.
         * If multiple movies are returned based on the movie name input, IDs & names will be sent to
         *  {@link DisplayMoviesForSelectionActivity} for the user to select the
         *  movie he wants to search from this list.
         *
         * @see android.os.AsyncTask#doInBackground(Object[])
         */
        @Override
        protected String doInBackground(String... params) {
            // Calls onProgressUpdate()
            publishProgress("Processing...");

            // Get the API key from the Assets folder
            String apiKey = PropertyUtil.returnRottenTomatoesAPIKeyFromAssets(SearchScreenActivity.this);
            if(isBlank(apiKey)){
                Log.e(SearchScreenActivity.class.getCanonicalName(), "apiKey is Blank");
            }
            else{
                String movieSearchResultJSON = HTTPUtil.returnSearchResultOrSimilarMoviesJSON(Constants.SEARCH_BY_MOVIE_NAME, enteredName, apiKey);
                if (null != movieSearchResultJSON) {
                    try {
                        // Get the "Movies" JSON array
                        JSONObject movieSearchResultJSONObj = new JSONObject(movieSearchResultJSON);
                        moviesJSONArray = movieSearchResultJSONObj.getJSONArray(Constants.TAG_MOVIES);
                        if (moviesJSONArray.length() == 0) {
                            return noMoviesFound;
                        } else {
                            if (moviesJSONArray.length() == 1) {
                                Intent intentSendMovieIDAndName = new Intent(SearchScreenActivity.this, DisplaySimilarMoviesActivity.class);

                                // Since we know there's only one movie; get the details from the first JSON object
                                intentSendMovieIDAndName.putExtra(
                                        Constants.INTENT_MOVIE_ID,
                                        moviesJSONArray.getJSONObject(0).getString(Constants.TAG_ID));

                                intentSendMovieIDAndName.putExtra(
                                        Constants.INTENT_MOVIE_NAME,
                                        moviesJSONArray.getJSONObject(0).getString(Constants.TAG_TITLE));

                                intentSendMovieIDAndName.putExtra(Constants.INTENT_KEY, apiKey);
                                startActivity(intentSendMovieIDAndName);

                                return oneResultFound;
                            } else {
                                // Send all the found movies to display for selection stored in a HashMap
                                HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<>();

                                JSONObject movieJSONObj;
                                for (int currentMovie = 0; currentMovie < moviesJSONArray.length(); currentMovie++) {
                                    movieJSONObj = moviesJSONArray.getJSONObject(currentMovie);

                                    List<String> listOfIDsAndNames = new ArrayList<>();
                                    listOfIDsAndNames.add(movieJSONObj.getString(Constants.TAG_ID));
                                    listOfIDsAndNames.add(movieJSONObj.getString(Constants.TAG_TITLE));

                                    movieIDNameHashCodeMap.put(
                                        movieJSONObj.getString(Constants.TAG_TITLE).hashCode(),
                                        listOfIDsAndNames);
                                }

                                Intent intentSendMovieIDsAndNames = new Intent(SearchScreenActivity.this, DisplayMoviesForSelectionActivity.class);
                                intentSendMovieIDsAndNames.putExtra(Constants.INTENT_MOVIE_ID_NAME, movieIDNameHashCodeMap);
                                intentSendMovieIDsAndNames.putExtra(Constants.INTENT_KEY, apiKey);
                                startActivity(intentSendMovieIDsAndNames);

                                return multipleResultsFound;
                            }
                        }
                    } catch (JSONException jsonException) {
                        Log.e(SearchScreenActivity.class.getCanonicalName(), jsonException.getLocalizedMessage());
                    } catch (Exception exception) {
                        Log.e(SearchScreenActivity.class.getCanonicalName(), exception.getLocalizedMessage());
                    }
                } else {
                    Log.d(SearchScreenActivity.class.getCanonicalName(), "API Returned 'null' for:" +enteredName); //$NON-NLS-1$
                    return noMoviesFound;
                }
            }
            return endOfProcessing;
        }

        /**
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String backgroundProcessingResult) {
            Intent intentToShowNoResults = new Intent(SearchScreenActivity.this, NoResultsActivity.class);

            switch (backgroundProcessingResult){
                case noMoviesFound:
                    Log.d(SearchScreenActivity.class.getCanonicalName(), noMoviesFound);
                    startActivity(intentToShowNoResults);
                    break;
                case oneResultFound:
                    Log.d(SearchScreenActivity.class.getCanonicalName(), oneResultFound);
                    break;
                case multipleResultsFound:
                    Log.d(SearchScreenActivity.class.getCanonicalName(), multipleResultsFound);
                    break;
                case endOfProcessing:
                    Log.d(SearchScreenActivity.class.getCanonicalName(), endOfProcessing);
                    break;
                default:
                    Log.d(SearchScreenActivity.class.getCanonicalName(), "onPostExecute:default"); //$NON-NLS-1$
                    startActivity(intentToShowNoResults);
                    break;
            }

            // After our processing, disable the spinner & reset the EditText
            spinner.setVisibility(View.GONE);
            editTextMovieName.setText(""); //$NON-NLS-1$
        }

        /**
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            /**
             * Things to be done before execution of long running operation.
             */

            // Save the entered name
            enteredName =  editTextMovieName.getText().toString().replace(' ', '+'); //$NON-NLS-2$
        }

        /**
         * @see android.os.AsyncTask#onProgressUpdate(Object[])
         */
        @Override
        protected void onProgressUpdate(String... text) {
            /**
             * Things to be done while execution of long running operation is in progress.
             * A ProgessDialog can be used here instead of a Spinner too
             */
            spinner.setVisibility(View.VISIBLE);
        }
    }
}