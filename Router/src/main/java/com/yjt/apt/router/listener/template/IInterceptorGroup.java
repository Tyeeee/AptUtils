package com.yjt.apt.router.listener.template;

import java.util.Map;

public interface IInterceptorGroup {
    
    void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptor);
}
