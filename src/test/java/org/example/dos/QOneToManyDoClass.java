package org.example.dos;

import com.querydsl.core.types.dsl.EntityPathBase;

public class QOneToManyDoClass extends EntityPathBase<OneToManyDoClass> {

    public static final QOneToManyDoClass oneToManyDoClass = new QOneToManyDoClass(
            "oneToManyDoClass");

    public QOneToManyDoClass(String variable) {
        super(OneToManyDoClass.class, variable);
    }
}
