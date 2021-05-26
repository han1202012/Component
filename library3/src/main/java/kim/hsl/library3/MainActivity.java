package kim.hsl.library3;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import kim.hsl.router_annotation.Extra;
import kim.hsl.router_annotation.Route;

@Route(path = "/library3/MainActivity")
public class MainActivity extends AppCompatActivity {

    /**
     * 姓名
     */
    @Extra
    public String name;

    /**
     * 年龄
     */
    @Extra
    public int age;

    /**
     * 身高
     */
    @Extra
    public int height;

    /**
     * 体重
     */
    @Extra
    public int weight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }
}