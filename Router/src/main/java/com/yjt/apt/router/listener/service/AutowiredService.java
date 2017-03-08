package com.yjt.apt.router.listener.service;

import com.yjt.apt.router.listener.template.IProvider;

public interface AutowiredService extends IProvider {

    void autowire(Object instance);
}
