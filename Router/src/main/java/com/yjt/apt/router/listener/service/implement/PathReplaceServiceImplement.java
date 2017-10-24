package com.yjt.apt.router.listener.service.implement;


import android.content.Context;
import android.net.Uri;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.listener.service.PathReplaceService;

@Route(path = "/router/service/replace")
public class PathReplaceServiceImplement implements PathReplaceService {
    
    @Override
    public void initialize(Context context) {

    }

    @Override
    public String forString(String path) {
        return path;
    }

    @Override
    public Uri forUri(Uri uri) {
        return uri;
    }
}
