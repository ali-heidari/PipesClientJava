package com.elpixeler.pipesclient;

import java.util.HashMap;

public interface Handler {
    public Object run(HashMap<String,Object> data);
}