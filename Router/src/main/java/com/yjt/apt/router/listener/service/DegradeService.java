package com.yjt.apt.router.listener.service;

import android.content.Context;

import com.yjt.apt.router.listener.template.IProvider;
import com.yjt.apt.router.model.Postcard;


public interface DegradeService extends IProvider {

    void onLost(Context context, Postcard postcard);
}
