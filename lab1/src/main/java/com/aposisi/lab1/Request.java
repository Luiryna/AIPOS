package com.aposisi.lab1;

import java.util.Arrays;
import java.util.Optional;

public enum Request {
    GET("GET"),
    POST("POST"),
    OPTIONS("OPTIONS");

    private String name;

    Request(String name){
        this.name = name;
    }

    public static Optional<Request> of(String name){
        return Arrays.stream(Request.values())
                .filter(x -> x.name.equalsIgnoreCase(name))
                .findFirst();
    }
}
