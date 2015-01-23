package invin.com.similarmovies;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * If the search for a movie from {@link invin.com.similarmovies.HomeScreenActivity} yields more
 * than one result, {@link invin.com.similarmovies.DisplayMoviesForSelectionActivity} will be called
 * to display a list of the movies returned by the RottenTomatoes API
 */
public class DisplayMoviesForSelectionActivity extends ListActivity {

    private static final String TAG_TITLE = "title";

    private ArrayList<String> movieListArray;
    private HashMap<Integer, List<String>> movieIDNameHashCodeMap = new HashMap<Integer, List<String>>();
    String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_movies_for_selection);

        //Logic to go back to the parent activity
        Button returnButton = (Button)findViewById(R.id.returnButtonFromMoviesForSelectionList);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // Get the movie ID & name passed in from the Home Screen
        Intent intent = getIntent();
        movieIDNameHashCodeMap = (HashMap<Integer, List<String>>) intent.getSerializableExtra(HomeScreenActivity.INTENT_MOVIE_ID_NAME);
        apiKey = intent.getStringExtra(HomeScreenActivity.INTENT_KEY);

        movieListArray = new ArrayList<String>();
        ListView movieListView = (ListView) findViewById(R.id.list_item);

        for (HashMap.Entry<Integer, List<String>> hashMovieEntry : movieIDNameHashCodeMap.entrySet()) {
            List<String> listOfIDsAndNames = hashMovieEntry.getValue();
            movieListArray.add(listOfIDsAndNames.get(1));
        }

        ArrayAdapter<String> movieListAdapter = new ArrayAdapter <String>(
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

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        //Get the selected movie
        String selectedItem = (String) getListView().getItemAtPosition(position);
        //Alternative method to get the selected movie:
        //String selectedItem = (String) getListAdapter().getItem(position);

        for (HashMap.Entry<Integer, List<String>> hashMovieEntry : movieIDNameHashCodeMap.entrySet()) {
            if(selectedItem.hashCode() == hashMovieEntry.getKey()){
                List<String> listOfIDsAndNames = hashMovieEntry.getValue();

                Intent intentSendMovieID = new Intent(this, DisplaySimilarMoviesListActivity.class);
                intentSendMovieID.putExtra(HomeScreenActivity.INTENT_MOVIE_ID, listOfIDsAndNames.get(0));
                intentSendMovieID.putExtra(HomeScreenActivity.INTENT_KEY, apiKey);
                startActivity(intentSendMovieID);
            }
        }

        //TODO: Implement a logger for debugging
        //Commenting out Toast; Uncomment to serve debugging purposes
        Toast.makeText(
                getApplicationContext(),
                "You clicked " + selectedItem + " at position " + position,
                Toast.LENGTH_SHORT).show();
    }
}