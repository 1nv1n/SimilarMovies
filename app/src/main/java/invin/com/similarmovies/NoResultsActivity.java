package invin.com.similarmovies;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Activity to display a 'No Results' screen.
 *
 * @author Neil Pathare
 */
public class NoResultsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_results);

        Button returnButton = (Button)findViewById(R.id.goHomeButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_no_results, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.action_settings:
                openActionSettings();
                return true;
            case R.id.action_about:
                openActionAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //TODO: Externalize this method into a Util package
    /**
     * Handle the 'Settings' action from the Action Bar
     */
    public void openActionSettings(){
        Toast.makeText(
                getApplicationContext(),
                "Sorry, Settings are currently disabled",
                Toast.LENGTH_SHORT).show();
    }

    //TODO: Externalize this method into a Util package
    /**
     * Handle the 'About' action from the Action Bar
     */
    public void openActionAbout(){
        Toast.makeText(
                getApplicationContext(),
                "Sorry, 'About' Currently Disabled",
                Toast.LENGTH_SHORT).show();
    }
}