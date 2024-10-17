package org.example.query;

import java.util.Set;

public record QueryParams(String fieldName, Set<Join> joins, Object value) {}
