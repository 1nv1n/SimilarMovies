package invin.com.similarmovies;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity to display all the movies found by the search
 */
public class DisplayMoviesForSelectionActivity extends ListActivity {

    private static final String TAG_TITLE = "title";
    public final static String EXTRA_MESSAGE;
    static{
        EXTRA_MESSAGE = "com.invin.similarmovies.MESSAGE";
    }

    //Hashmap for ListView
    ArrayList<HashMap<String, String>> movieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_movies_for_selection);

        Bundle movieIDBundle = this.getIntent().getExtras();
        String[] movieSearchResultIDStringArray = movieIDBundle.getStringArray(EXTRA_MESSAGE);

        movieList = new ArrayList<HashMap<String, String>>();

        for(int numberOfMovies = 0; numberOfMovies < movieSearchResultIDStringArray.length; numberOfMovies++){
            HashMap<String, String> movieHashMap = new HashMap<String, String>();
            movieHashMap.put(TAG_TITLE, movieSearchResultIDStringArray[numberOfMovies]);
            movieList.add(movieHashMap);
        }

        ListAdapter adapter = new SimpleAdapter(
                DisplayMoviesForSelectionActivity.this,
                movieList,
                R.layout.list_item,
                new String[] { TAG_TITLE },
                new int[] { R.id.movie_title });

        setListAdapter(adapter);
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
}