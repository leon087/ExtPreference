package cm.android.preference.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cm.android.extpreference.sample.R;
import cm.android.preference.ext.ExtPreferences;
import cm.android.preference.security.PreferenceFactory;

public class MainActivity extends Activity {

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button refreshView = (Button) this.findViewById(R.id.refresh);
        refreshView.setClickable(true);
        refreshView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
                refresh2();
            }
        });

        testExt();
    }

    private void refresh() {
        SharedPreferences preferences = PreferenceFactory.getPreferences(this, "test_pref_1", "111", "pwd");

        SharedPreferences.Editor editor = preferences.edit();
        String key = "ggg_key_str" + new Random().nextInt(100);
        editor.putString(key, "ggg_value" + new Random().nextInt(100));
        editor.commit();

        String str = preferences.getString(key, "");

        TextView keyView = (TextView) this.findViewById(R.id.key);
        TextView valueView = (TextView) this.findViewById(R.id.value);
        keyView.setText(key);
        valueView.setText(str);
        android.util.Log.e("ggg", "ggg key = " + key);
        android.util.Log.e("ggg", "ggg map = " + preferences.getAll());
    }

    private void refresh2() {
        SharedPreferences preferences = PreferenceFactory.getPreferences(this, "test_pref_2", "111", "pwd");

        SharedPreferences.Editor editor = preferences.edit();
        String key = "ggg_key_str" + new Random().nextInt(100);
        editor.putString(key, "ggg_value" + new Random().nextInt(100));
        editor.commit();
    }

    private void testSecurity() {
    }

    private void testExt() {
        Set<String> set = new HashSet<>();
        set.add("aaa");
        set.add("bbb");

        SharedPreferences pfs = ExtPreferences.getSharedPreferences(this, "prefs_ggg");
        pfs.edit()
                .putString("vs", "123")
                .putLong("vl", Long.MAX_VALUE)
                .putInt("vi", 123)
                .putFloat("vf", 11.1f)
                .putBoolean("vb", true)
                .putStringSet("vset", set)
                .apply();


        SharedPreferences rpfs = ExtPreferences.getSharedPreferences(this, "prefs_ggg");
        Map<String, ?> map = rpfs.getAll();
        android.util.Log.e("ggggg", "ggggg map = " + map);
    }
}
