package kim.hsl.route_core;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexFile;
import kim.hsl.route_core.template.IRouteGroup;
import kim.hsl.route_core.template.IRouteRoot;

public class Router {
    private static final String TAG = "Router";

    /**
     * 上下文
     */
    private static Application mContext;

    /**
     * 单例类
     */
    private static Router instance;

    private Router() {
    }

    /**
     * 初始化路由表
     * @param application
     */
    public static void init(Application application) {
        mContext = application;
        loadInfo();
    }

    /**
     * 加载 分组 路由表 数据
     * 每个分组对应一个路由表
     */
    private static void loadInfo(){
        /*
            获取程序的所有 APK 安装文件
         */
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = mContext.getPackageManager().getApplicationInfo(mContext
                    .getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // 使用集合存放应用安装的 APK 文件
        List<String> sourcePaths = new ArrayList<>();

        // 一般情况下 , 一个应用只有一个安装 APK
        sourcePaths.add(applicationInfo.sourceDir);

        // 如果是 instant run 形式安装的 , 则有多个 APK 文件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (null != applicationInfo.splitSourceDirs) {
                sourcePaths.addAll(Arrays.asList(applicationInfo.splitSourceDirs));
            }
        }


        /*
            根据获取所有 APK 下的类
            根据 kim.hsl.router 包名, 获取该包名下的所有路由类
         */

        // 获取查找的 kim.hsl.router 包下的类 的 类名
        Set<String> classNames = new HashSet<>();

        // 遍历所有的 APK 路径 , 查找其中的 DEX 中的类
        for (final String path : sourcePaths) {
            // 获取 APK 下的 dex 文件
            DexFile dexfile = null;
            try {
                dexfile = new DexFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Enumeration<String> dexEntries = dexfile.entries();
            // 遍历 DEX 文件中的所有的类
            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
                if (className.startsWith("kim.hsl.router")) {
                    classNames.add(className);
                }
            }
        }

        // 最终所有的 kim.hsl.router 包下的类都存放到了 Set<String> classNames 变量中
        for (String className : classNames){
            /*
                这是打印出来的类
                kim.hsl.router_annotation.model.RouteBean$Type
                kim.hsl.router.Router_Group_app
                kim.hsl.router_annotation.Route
                kim.hsl.router.Router_Root_library2
                kim.hsl.router.Router_Root_app
                kim.hsl.router.Router_Group_library2
                kim.hsl.router_annotation.model.RouteBean
             */
            Log.i(TAG, "loadInfo : " + className);

            // 如果该类以 " Router_Root_ " 开头 , 说明这是 Root 表类
            if (className.startsWith("kim.hsl.router.Router_Root_")) {
                // root中注册的是分组信息 将分组信息加入仓库中
                try {
                    // 获取 IRouteRoot 类
                    Class<IRouteRoot> clazz = (Class<IRouteRoot>) Class.forName(className);

                    // 获取构造函数
                    Constructor<IRouteRoot> constructor = clazz.getConstructor();

                    // 创建 IRouteRoot 类
                    IRouteRoot routeRoot = constructor.newInstance();

                    // 将 Root 表的信息装载到 Warehouse.groupsIndex 集合中
                    routeRoot.loadInto(Warehouse.groupsIndex);

                    // 打印 Root 表
                    for ( Map.Entry<String, Class<? extends IRouteGroup>> entry : Warehouse.groupsIndex.entrySet()){
                        Log.i(TAG, "loadInfo : " + entry.getKey() + " : " + entry.getValue().getName());
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
