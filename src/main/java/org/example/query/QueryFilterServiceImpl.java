package org.example.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.stereotype.Service;

@Service
public class QueryFilterServiceImpl implements QueryFilterService {

  @Override
  public <T> Predicate defaultPredicate(JPAQuery<?> query, EntityPathBase<T> rootEntity) {
    return new BooleanBuilder();
  }
}
