package com.yjt.apt.router;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.yjt.apt.R;
import com.yjt.apt.router.listener.callback.NavigationCallback;
import com.yjt.apt.router.model.Postcard;

public class RouterActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.init:
                Router.getInstance().initialize(getApplication());
                break;
            case R.id.openLog:
                Router.getInstance().openLog();
                break;
            case R.id.openDebug:
                Router.getInstance().openDebug();
                break;
            case R.id.normalNavigation:
                Router.getInstance().build("/router/activity2").navigation(this);
                break;
            case R.id.normalNavigationWithParams:
                Router.getInstance().build("/router/activity2").putString("key1", "value1").navigation(this);
                break;
            case R.id.interceptor:
                Router.getInstance().build("/router/activity4").navigation(this);
                break;
            case R.id.navByUrl:
                Router.getInstance().build("/router/WebviewActivity").putString("url", "file:///android_asset/schame-test.html").navigation(this);
                break;
            case R.id.autoInject:
                Router.getInstance().enableAutoInject();
                break;
            case R.id.navByName:
//                ((HelloService) Router.getInstance().build("/service/hello").navigation()).sayHello("mike");
                break;
            case R.id.navByType:
//                Router.getInstance().navigation(HelloService.class).sayHello("mike");
                break;
            case R.id.navToMoudle1:
                Router.getInstance().build("/module/1").navigation(this);
                break;
            case R.id.navToMoudle2:
                // 这个页面主动指定了Group名
                Router.getInstance().build("/module/2", "m2").navigation(this);
                break;
            case R.id.destroy:
                Router.getInstance().destroy();
                break;
            case R.id.failNav:
                Router.getInstance().build("/xxx/xxx").navigation(this, new NavigationCallback() {

                    @Override
                    public void onFound(Postcard postcard) {

                    }

                    @Override
                    public void onLost(Postcard postcard) {
                        Log.d("Router", "找不到了");
                    }
                });
                break;
            case R.id.failNav2:
                Router.getInstance().build("/xxx/xxx").navigation(this);
                break;
            case R.id.failNav3:
                Router.getInstance().navigation(RouterActivity.class);
                break;
            case R.id.normalNavigation2:
                Router.getInstance().build("/router/activity2").navigation(this, 666);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 666:
                Toast.makeText(this, String.valueOf(resultCode), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}
