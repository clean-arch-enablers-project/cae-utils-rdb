package com.cae.rdb.queries;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Params {

    private final List<Param> params = new ArrayList<>();

    public static Params builder() {
        return new Params();
    }

    public void add(String field, Object value) {
        this.params.add(Param.of(field, value));
    }

    public List<Param> build() {
        return Collections.unmodifiableList(params);
    }

    public static List<Param> of(String field1, Object value1){
        return List.of(Param.of(field1, value1));
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2){
        return List.of(
            Param.of(field1, value1),
            Param.of(field2, value2)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5,
            String field6, Object value6){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5),
                Param.of(field6, value6)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5,
            String field6, Object value6,
            String field7, Object value7){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5),
                Param.of(field6, value6),
                Param.of(field7, value7)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5,
            String field6, Object value6,
            String field7, Object value7,
            String field8, Object value8){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5),
                Param.of(field6, value6),
                Param.of(field7, value7),
                Param.of(field8, value8)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5,
            String field6, Object value6,
            String field7, Object value7,
            String field8, Object value8,
            String field9, Object value9){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5),
                Param.of(field6, value6),
                Param.of(field7, value7),
                Param.of(field8, value8),
                Param.of(field9, value9)
        );
    }

    public static List<Param> of(
            String field1, Object value1,
            String field2, Object value2,
            String field3, Object value3,
            String field4, Object value4,
            String field5, Object value5,
            String field6, Object value6,
            String field7, Object value7,
            String field8, Object value8,
            String field9, Object value9,
            String field10, Object value10){
        return List.of(
                Param.of(field1, value1),
                Param.of(field2, value2),
                Param.of(field3, value3),
                Param.of(field4, value4),
                Param.of(field5, value5),
                Param.of(field6, value6),
                Param.of(field7, value7),
                Param.of(field8, value8),
                Param.of(field9, value9),
                Param.of(field10, value10)
        );
    }

}
