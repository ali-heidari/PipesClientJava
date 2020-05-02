package com.elpixeler.pipesclient;

import com.google.gson.internal.LinkedTreeMap;

public interface Handler {
    public Object run(LinkedTreeMap<String,Object> data);
}