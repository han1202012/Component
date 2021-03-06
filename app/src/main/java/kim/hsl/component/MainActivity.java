package kim.hsl.component;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import kim.hsl.route_core.Router;
import kim.hsl.router_annotation.Route;

@Route(path = "/app/MainActivity")
public class MainActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化路由表
        Router.init(getApplication());
    }
}
