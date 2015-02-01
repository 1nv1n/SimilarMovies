package invin.com.similarmovies;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

/**
 * Display an 'About' screen with App information
 */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        /**
         * On click of the 'Return' button, finish the current activity & return to the previous open activity
         */
        Button returnButtonFromAboutActivity = (Button)findViewById(R.id.returnButtonFromAboutActivity);
        returnButtonFromAboutActivity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        /**
         * On click of the 'Changelog' butting, open the Changelog activity
         */
        Button viewChangelogButton = (Button)findViewById(R.id.viewChangelogButton);
        viewChangelogButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentShowChangelog = new Intent(AboutActivity.this, ChangelogActivity.class);
                startActivity(intentShowChangelog);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }
}
