package ragone.io.quietmind;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.ConfigurationCompat;

import java.util.Locale;

import ragone.io.quietmind.fragment.MyFragment;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Prefs prefs = new Prefs(this);
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        ((TextView) findViewById(R.id.currentstreak)).setText(StatsFormatter.formatDays(prefs.getStreak()));
        ((TextView) findViewById(R.id.longeststreak)).setText(StatsFormatter.formatDays(prefs.getLongestStreak()));
        ((TextView) findViewById(R.id.totaltime)).setText(
                StatsFormatter.formatTotalTime(prefs.getTotalMinutes(), locale));
        ((TextView) findViewById(R.id.averagetime)).setText(
                StatsFormatter.formatAverageTime(prefs.getAverageMinutes()));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MyFragment())
                    .commit();
        }
    }
}
