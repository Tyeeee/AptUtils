package com.yjt.apt.test.router;

import com.yjt.apt.router.listener.template.IProvider;

public interface TestService extends IProvider {

    void hello(String name);
}
