package com.yjt.apt.router.listener.template;

import java.util.Map;

public interface IRouteRoot {

    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}
