package io.awssample.handler;

import java.math.BigDecimal;

record CreateProductRequest(String name, BigDecimal price) {
}
