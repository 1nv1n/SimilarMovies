package invin.com.similarmovies.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import invin.com.similarmovies.BuildConfig;
import invin.com.similarmovies.R;
import invin.com.similarmovies.util.Constants;

/**
 * If the search for a movie from {@link SearchScreenActivity} yields more than one result,
 * {@link DisplayMoviesForSelectionActivity} will be called to display a list of the movies
 * returned by the RottenTomatoes API
 */
public class DisplayMoviesForSelectionActivity extends ListActivity {

    // Hash map of the movie name, it's RottenTomatoes ID with the hash code of the name as the key
    private HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<Integer, List<String>>();

    // Store the API key (To pass on to the next activity)
    private String apiKey;

    // Verify that hash-coding served its purpose
    private boolean wasMovieMatched;

    // List of movies
    private ArrayList<String> movieListArray;

    public DisplayMoviesForSelectionActivity() {
        wasMovieMatched = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_movies_for_selection);

        /**
         * Finish the current activity & return to the previous open activity
         */
        Button returnButton = (Button)findViewById(R.id.returnButtonFromMoviesForSelectionList);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // Get the movie ID, name & the API key passed in from the Home Screen
        Intent intentSendMovieIDsAndNames = getIntent();
        movieIDNameHashCodeMap = (HashMap<Integer, List<String>>) intentSendMovieIDsAndNames.getSerializableExtra(Constants.INTENT_MOVIE_ID_NAME);
        apiKey = intentSendMovieIDsAndNames.getStringExtra(Constants.INTENT_KEY);

        movieListArray = new ArrayList<>();
        for (HashMap.Entry<Integer, List<String>> hashMovieEntry : movieIDNameHashCodeMap.entrySet()) {
            List<String> listOfIDsAndNames = hashMovieEntry.getValue();
            movieListArray.add(listOfIDsAndNames.get(1));
        }

        ArrayAdapter<String> movieListAdapter = new ArrayAdapter<>(
                this,
                R.layout.movies_list,
                R.id.movieTitle,
                movieListArray);

        setListAdapter(movieListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_movies_for_selection, menu);
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

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        // Get the selected movie
        String selectedItem = (String) getListView().getItemAtPosition(position);

        // Alternative method to get the selected movie:
        // String selectedItem = (String) getListAdapter().getItem(position);

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
                break;
            }
        }

        if(!wasMovieMatched){
            Toast.makeText(getApplicationContext(), R.string.text_error_restart, Toast.LENGTH_SHORT).show();
        }
    }
}