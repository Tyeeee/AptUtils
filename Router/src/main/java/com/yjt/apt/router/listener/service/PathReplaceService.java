package com.yjt.apt.router.listener.service;

import android.net.Uri;

import com.yjt.apt.router.listener.template.IProvider;


public interface PathReplaceService extends IProvider {

    String forString(String path);

    Uri forUri(Uri uri);
}
