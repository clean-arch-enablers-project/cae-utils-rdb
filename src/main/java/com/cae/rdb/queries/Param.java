package com.cae.rdb.queries;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Param {

    public static Param of(String field, Object value){
        return new Param(field, value);
    }

    private final String field;
    private final Object value;

}
