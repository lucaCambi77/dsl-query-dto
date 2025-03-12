package org.example;

import org.example.dos.OneToManyDoClass;
import org.example.dos.QOneToManyDoClass;
import org.example.query.service.QueryFilterService;

import java.util.Map;

public class QueryFilterServiceImpl implements QueryFilterService {

    public final static String ONE_TO_MANY_DO = "ONE_TO_MANY_DO";

    public static final String FAKE_DO = "FakeDo"; // to trigger missing expand in tests

    private static final Map<String, String> expandMap = Map.ofEntries(
            Map.entry(ONE_TO_MANY_DO, QOneToManyDoClass.oneToManyDoClass.getMetadata().getName()));


    private static final Map<String, Class<?>> doMap = Map.ofEntries(
            Map.entry(ONE_TO_MANY_DO, OneToManyDoClass.class),
            Map.entry(FAKE_DO, OneToManyDoClass.class)
    );

    @Override
    public String entityFrom(String expand) {
        return expandMap.get(expand);
    }

    @Override
    public Class<?> doFrom(String relation) {
        return doMap.get(relation);
    }
}
