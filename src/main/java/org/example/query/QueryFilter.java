package org.example.query;

import com.querydsl.core.types.Path;

public record QueryFilter(Path<?> path, Object value) {
}
