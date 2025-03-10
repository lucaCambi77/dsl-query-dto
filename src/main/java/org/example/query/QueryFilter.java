package org.example.query;

import java.util.List;

public record QueryFilter(String fieldName, List<Join> joins, Object value) {

}
