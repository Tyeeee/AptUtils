package com.yjt.apt.router.listener.template;

import com.yjt.apt.router.annotation.model.RouteMetadata;

import java.util.Map;

public interface IProviderGroup {
    
    void loadInto(Map<String, RouteMetadata> providers);
}