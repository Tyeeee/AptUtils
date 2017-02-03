package com.yjt.apt.router.listener.template;


import com.yjt.apt.router.annotation.model.RouteMetadata;

import java.util.Map;

public interface IRouteGroup {

    void loadInto(Map<String, RouteMetadata> atlas);
}
