package kim.hsl.library2;

import android.util.Log;

import kim.hsl.base.ComponentService;
import kim.hsl.router_annotation.Route;

@Route(path = "/library2/StringService")
public class StringService implements ComponentService {

    @Override
    public void doSomething() {
        Log.i("StringService", "library2 组件中的 StringService 服务 ");
    }
}
