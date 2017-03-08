package com.yjt.apt.router.listener.service;

import com.yjt.apt.router.listener.template.IProvider;

public interface ClassLoaderService extends IProvider {
    
    Class<?> forName();
}
