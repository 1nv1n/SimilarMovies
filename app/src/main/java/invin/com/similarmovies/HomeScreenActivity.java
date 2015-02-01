package invin.com.similarmovies;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import invin.com.similarmovies.util.Constants;

import static invin.com.similarmovies.R.menu.menu_main;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Home Screen of the App.
 *
 * @author Neil Pathare
 */
public class HomeScreenActivity extends Activity {

    //To show a loading indicator between Activities whilst data processing is ongoing
    private ProgressBar spinner;

    //JSON tags
    public final static String TAG_ID;
    public final static String TAG_TITLE;
    public final static String TAG_MOVIES;

    //Different intents in use
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
    private JSONArray moviesJSONArray;

    //Strings to assist in the API URL build
    private String apiURLPrepender;
    private String apiMovieSearchQueryAppender;
    private String apiMovieSearchConnector;

    //EditText to get the movie name from the user
    private EditText editTextMovieName;

    //Basic constructor
    public HomeScreenActivity() {
        moviesJSONArray = null;
        apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
        apiMovieSearchQueryAppender = ".json?q=";
        apiMovieSearchConnector = "&page_limit=10&page=1&apikey=";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        //Default 'Up - Home' button functionality is intentionally disabled
        getActionBar().setDisplayHomeAsUpEnabled(false);

        //Create a loading spinner & leave it hidden until required
        spinner = (ProgressBar)findViewById(R.id.homeScreenProgressBar);
        spinner.setVisibility(View.GONE);

        /**
         * Identify the EditText & set an Action Listener to automatically pass the contents
         * to the relevant method while disabling the 'Enter' key
         */
        editTextMovieName = (EditText) findViewById(R.id.editTextMovieName);
        editTextMovieName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                boolean handledIMEAction = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if(isAValidMovieName(editTextMovieName.getText().toString())){
                        //Start a new AsyncTaskRunner to ensure that we always process on a new thread
                        new AsyncTaskRunner().execute(editTextMovieName.getText().toString());
                        handledIMEAction = true;
                    }
                }
                return handledIMEAction;
            }
        });

        Button searchMovieButton = (Button)findViewById(R.id.searchMovieButton);
        searchMovieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAValidMovieName(editTextMovieName.getText().toString())){
                    //Start a new AsyncTaskRunner to ensure that we always process on a new thread
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
            //Commenting out Settings for now.
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

    /**
     * Makes the HTTP call to the API & returns the JSON response as a {@link java.lang.String}
     *
     * @param movieToSearchFor {@link java.lang.String} consisting of the movie name
     * @param apiKey {@link java.lang.String} consisting of the API key
     * @return {@link java.lang.String} consisting of the JSON response from the Rotten Tomatoes API
     */
    private String returnSearchResultJSON(String movieToSearchFor, String apiKey) {
        HttpClient defaultHTTPClient = new DefaultHttpClient();

        String apiResponse = "defaultAPIResponse";

        HttpGet movieSearchHTTPRequest = new HttpGet(
                apiURLPrepender +
                apiMovieSearchQueryAppender +
                movieToSearchFor +
                apiMovieSearchConnector +
                apiKey);

        HttpResponse movieSearchHTTPResponse;

        InputStreamReader apiResponseInputStreamReader = null;
        BufferedReader inputStreamBufferedReader = null;
        InputStream inputStreamFromAPIResponse = null;

        try {
            movieSearchHTTPResponse = defaultHTTPClient.execute(movieSearchHTTPRequest);
            //As long as we don't get a null response, continue processing
            if(!movieSearchHTTPResponse.equals(null)){
                inputStreamFromAPIResponse = movieSearchHTTPResponse.getEntity().getContent();

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
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "ClientProtocolException:" + e.toString());
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "IOException:" + e.toString());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "Exception:" + e.toString());
            }
        } finally {
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
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "UnsupportedEncodingException:" + e.toString());
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "IOException:" + e.toString());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOG, "Exception:" + e.toString());
            }
        } finally {
            IOUtils.closeQuietly(propertiesReader);
            IOUtils.closeQuietly(bufferedAssetsFileReader);
            return keyBuilder.toString();
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
     * Verify that the entered movie name follows guidelines
     *
     * @param enteredMovieName 9{@link java.lang.String} containing the movie name)
     * @return TRUE if the entered text is valid, FALSE otherwise
     */
    private boolean isAValidMovieName(String enteredMovieName){

        int MAXIMUM_MOVIE_NAME_LENGTH = 225;

        if(isBlank(enteredMovieName)) {
            Toast.makeText(
                    getApplicationContext(),
                    "You need to enter a movie name first..",
                    Toast.LENGTH_SHORT).show();

            //Reset the EditText & hide the loading spinner
            editTextMovieName.setText("");
            spinner.setVisibility(View.GONE);

            return false;
        }
        else if (!enteredMovieName.matches("[a-zA-Z0-9.?-_: ]*")) {
            Toast.makeText(
                    getApplicationContext(),
                    "Please re-enter the name without special characters.",
                    Toast.LENGTH_SHORT).show();

            //Reset the EditText & hide the loading spinner
            editTextMovieName.setText("");
            spinner.setVisibility(View.GONE);

            return false;
        }
        else if (enteredMovieName.length() > MAXIMUM_MOVIE_NAME_LENGTH) {

            Toast.makeText(
                    getApplicationContext(),
                    "Entered text seems a bit long. Are you sure it's the name of a movie?",
                    Toast.LENGTH_SHORT).show();

            //Reset the EditText & hide the loading spinner
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

        private final String noMoviesFound = "noMoviesFound";
        private final String oneResultFound = "oneResultFound";
        private final String endOfProcessing = "endOfProcessing";
        private final String multipleResultsFound = "multipleResultsFound";

        /**
         * This method will attempt to retrieve the ID of the entered movie & send that to
         *  {@link invin.com.similarmovies.DisplaySimilarMoviesListActivity} to search for & display
         *  similar movies.
         * If multiple movies are returned based on the movie name input, IDs & names will be sent to
         *  {@link invin.com.similarmovies.DisplayMoviesForSelectionActivity} for the user to select the
         *  movie he wants to search from this list.
         *
         * @see android.os.AsyncTask#doInBackground(Object[])
         */
        @Override
        protected String doInBackground(String... params) {
            // Calls onProgressUpdate()
            publishProgress("Processing...");

            // Get the API key from the Assets folder
            String apiKey = returnRottenTomatoesAPIKeyFromAssets();

            if(isBlank(apiKey)){
                System.out.println("apiKey is Blank");
            }
            else{
                String movieSearchResultJSON;
                int numberOfMoviesFound;

                movieSearchResultJSON = returnSearchResultJSON(
                    editTextMovieName.getText().toString().replace(' ', '+'),
                        apiKey);

                if (null != movieSearchResultJSON) {
                    try {
                        JSONObject movieSearchResultJSONObj = new JSONObject(movieSearchResultJSON);
                        JSONObject movieJSONObj;

                        // Get the "Movies" JSON array
                        moviesJSONArray = movieSearchResultJSONObj.getJSONArray(TAG_MOVIES);
                        numberOfMoviesFound = moviesJSONArray.length();

                        if (numberOfMoviesFound == 0) {
                            return noMoviesFound;
                        } else {
                            if (numberOfMoviesFound == 1) {
                                Intent intentSendMovieIDAndName = new Intent(HomeScreenActivity.this, DisplaySimilarMoviesListActivity.class);

                                //Since we know there's only one movie; get the details from the first JSON object
                                intentSendMovieIDAndName.putExtra(
                                    INTENT_MOVIE_ID,
                                    moviesJSONArray.getJSONObject(0).getString(TAG_ID));

                                intentSendMovieIDAndName.putExtra(
                                    INTENT_MOVIE_NAME,
                                    moviesJSONArray.getJSONObject(0).getString(TAG_TITLE));

                                intentSendMovieIDAndName.putExtra(INTENT_KEY, apiKey);
                                startActivity(intentSendMovieIDAndName);

                                return oneResultFound;
                            } else {
                                //Send all the found movies to display for selection stored in a HashMap
                                HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<>();

                                for (int currentMovie = 0; currentMovie < numberOfMoviesFound; currentMovie++) {
                                    movieJSONObj = moviesJSONArray.getJSONObject(currentMovie);

                                    List<String> listOfIDsAndNames = new ArrayList<>();
                                    listOfIDsAndNames.add(movieJSONObj.getString(TAG_ID));
                                    listOfIDsAndNames.add(movieJSONObj.getString(TAG_TITLE));

                                    movieIDNameHashCodeMap.put(
                                        movieJSONObj.getString(TAG_TITLE).hashCode(),
                                        listOfIDsAndNames);
                                }

                                Intent intentSendMovieIDsAndNames = new Intent(HomeScreenActivity.this, DisplayMoviesForSelectionActivity.class);
                                intentSendMovieIDsAndNames.putExtra(INTENT_MOVIE_ID_NAME, movieIDNameHashCodeMap);
                                intentSendMovieIDsAndNames.putExtra(INTENT_KEY, apiKey);
                                startActivity(intentSendMovieIDsAndNames);

                                return multipleResultsFound;
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(Constants.LOG, "JSONException:" + movieSearchResultJSON);
                        }
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(Constants.LOG, "Exception:" + movieSearchResultJSON);
                        }
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, "API Returned 'null' for:" + editTextMovieName.getText().toString().replace(' ', '+'));
                    }
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
            Intent intentToShowNoResults = new Intent(HomeScreenActivity.this, NoResultsActivity.class);

            switch (backgroundProcessingResult){
                case noMoviesFound:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, noMoviesFound);
                    }
                    startActivity(intentToShowNoResults);
                    break;
                case oneResultFound:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, oneResultFound);
                    }
                    break;
                case multipleResultsFound:
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOG, multipleResultsFound);
                    }
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
            //After our processing, disable the spinner & reset the EditText
            spinner.setVisibility(View.GONE);
            editTextMovieName.setText("");
        }

        /**
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            /**
             * Things to be done before execution of long running operation.
             */
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