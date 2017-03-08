package com.yjt.apt.router.utils.wrapper;

import java.util.TreeMap;

public class UniqueKeyTreeMap<K, V> extends TreeMap<K, V> {
    
    private String prompt;

    public UniqueKeyTreeMap(String prompt) {
        super();
        this.prompt = prompt;
    }

    @Override
    public V put(K key, V value) {
        if (containsKey(key)) {
            throw new RuntimeException(String.format(prompt, key));
        } else {
            return super.put(key, value);
        }
    }
}
