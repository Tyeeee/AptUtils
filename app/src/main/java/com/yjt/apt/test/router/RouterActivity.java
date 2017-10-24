package com.yjt.apt.test.router;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.yjt.apt.R;
import com.yjt.apt.constant.Constant;
import com.yjt.apt.router.Router;
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
            case R.id.openLog:
                Router.getInstance().openLog();
                break;
            case R.id.openDebug:
                Router.getInstance().openDebug();
                break;
            case R.id.initialize:
                Router.getInstance().initialize(getApplication());
                break;
            case R.id.destroy:
                Router.getInstance().destroy();
                break;
            case R.id.startActivity:
                Router.getInstance().build("/test/router/TestActivity1").navigation(this);
                break;
            case R.id.startActivityForResult:
                Router.getInstance().build("/test/router/TestActivity2").navigation(this, Constant.REQUEST_CODE);
                break;
            case R.id.startActivityWithParameters:
                TestModel model = new TestModel();
                model.setName("JinTai Yuan");
                model.setLength(123);
                model.setWidth(321);
                model.setHeight(213);
                Router.getInstance().build("/test/router/TestActivity1").putParcelable("model", model).navigation(this);
                break;
            case R.id.startActivityByUrl:
                Router.getInstance().build("/test/router/TestActivity3").putString("url", "file:///android_asset/scheme-test.html").navigation(this);
                break;
            case R.id.autoInject:
//                Router.getInstance().enableAutoInject();
                break;
            case R.id.interceptor:
                Router.getInstance().build("/module/TestActivity4").navigation(this, new NavigationCallback() {

                    @Override
                    public void onFound(Postcard postcard) {
                        Toast.makeText(getApplicationContext(), "单独定义额外提示:onFound", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLost(Postcard postcard) {
                        Toast.makeText(getApplicationContext(), "单独定义额外提示:onLost", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case R.id.startServiceByName:
                ((TestService) Router.getInstance().build("/service/hello").navigation(this)).hello("袁锦泰");
//                Router.getInstance().build("//module/TestActivity7").navigation(this);
                break;
            case R.id.startServiceByType:
                Router.getInstance().navigation(TestService.class).hello("袁锦泰");
                break;
            case R.id.navigateToMoudle1:
                Router.getInstance().build("/module/TestActivity4").navigation(this);
                break;
            case R.id.navigateToMoudle2:
                Router.getInstance().build("/module/TestActivity5", "袁锦泰").navigation(this);
                break;
            case R.id.navigateFailedForDowngradeAlone:
                Router.getInstance().build("/module/TestActivity4", "袁锦泰").navigation(this, new NavigationCallback() {

                    @Override
                    public void onFound(Postcard postcard) {
                        Toast.makeText(getApplicationContext(), "单独定义额外提示:onFound", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLost(Postcard postcard) {
                        Toast.makeText(getApplicationContext(), "单独定义额外提示:onLost", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case R.id.navigateFailedForDowngradeAll:
                Router.getInstance().build("/module/TestActivity4", "袁锦泰").navigation(this);
                break;
            case R.id.navigateFailed:
                Router.getInstance().navigation(RouterActivity.class);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case Constant.RESULT_CODE:
                switch (requestCode) {
                    case Constant.REQUEST_CODE:
                        Toast.makeText(this, String.valueOf(resultCode), Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }
}
