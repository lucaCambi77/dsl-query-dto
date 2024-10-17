package org.example.query;

import java.util.Set;

public record QueryFilter(String fieldName, Set<Join> joins, Object value) {}
