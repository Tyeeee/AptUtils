package com.yjt.apt.router.listener.service;

import com.yjt.apt.router.listener.template.IProvider;

public interface AutowireService extends IProvider {

    void autowire(Object instance);
}
