package io.joel.impl.node;

import jakarta.el.ELContext;

sealed public interface BooleanExpression extends ExpressionNode permits BooleanNode, RelationalNode {
    @Override
    default Class<?> getType(ELContext context) {
        return Boolean.class;
    }
}
