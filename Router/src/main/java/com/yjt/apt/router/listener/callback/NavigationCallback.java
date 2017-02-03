package com.yjt.apt.router.listener.callback;


import com.yjt.apt.router.model.Postcard;

public interface NavigationCallback {

    void onFound(Postcard postcard);

    void onLost(Postcard postcard);
}
