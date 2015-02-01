package invin.com.similarmovies;

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

import invin.com.similarmovies.util.Constants;

/**
 * If the search for a movie from {@link invin.com.similarmovies.HomeScreenActivity} yields more
 * than one result, {@link invin.com.similarmovies.DisplayMoviesForSelectionActivity} will be called
 * to display a list of the movies returned by the RottenTomatoes API
 */
public class DisplayMoviesForSelectionActivity extends ListActivity {

    //Hashmap of the movie name, it's RottenTomatoes ID with the hash code of the name as the key
    private HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<Integer, List<String>>();

    //Store the API key (To pass on to the next activity)
    private String apiKey;

    //Verify that hash-coding served its purpose
    private boolean wasMovieMatched;

    ArrayList<String> movieListArray;

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
        movieIDNameHashCodeMap = (HashMap<Integer, List<String>>) intentSendMovieIDsAndNames.getSerializableExtra(HomeScreenActivity.INTENT_MOVIE_ID_NAME);
        apiKey = intentSendMovieIDsAndNames.getStringExtra(HomeScreenActivity.INTENT_KEY);

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
            case R.id.action_about:
                openActionAbout();
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

                Intent intentSendMovieNameAndID = new Intent(this, DisplaySimilarMoviesListActivity.class);
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_MOVIE_NAME, selectedItem);
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_MOVIE_ID, listOfIDsAndNames.get(0));
                intentSendMovieNameAndID.putExtra(HomeScreenActivity.INTENT_KEY, apiKey);

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOG, "Selected Movie:"+selectedItem);
                    Log.d(Constants.LOG, "Selected Movie ID:"+listOfIDsAndNames.get(0));
                }

                startActivity(intentSendMovieNameAndID);
                break;
            }
        }

        if(!wasMovieMatched){
            Toast.makeText(
                    getApplicationContext(),
                    "Something went wrong, please restart the App",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the 'About' action from the Action Bar
     */
    public void openActionAbout(){
        Intent intentToShowAboutActivity = new Intent(this, AboutActivity.class);
        startActivity(intentToShowAboutActivity);
    }
}