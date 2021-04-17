package io.joel.impl.spi;

import io.joel.impl.JoelValueExpression;
import io.joel.impl.antlr.JoelExpressionParser;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.util.Objects;

import static java.lang.System.Logger.Level.INFO;

public class JoelExpressionFactory extends ExpressionFactory {
    private static final System.Logger LOGGER = System.getLogger(JoelExpressionFactory.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public <T> T coerceToType(Object object, Class<T> targetType) {
        return (T) io.joel.impl.TypeConverter.coerce(object, targetType);
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context, String expression, Class<?> expectedReturnType, Class<?>[] expectedParamTypes) {
        Objects.requireNonNull(expectedParamTypes);
        return null;
    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
        Objects.requireNonNull(expectedType);
        LOGGER.log(INFO, "[JOEL] Evaluating expression: " + expression);
        var parseResult = JoelExpressionParser.parse(expression);
        return new JoelValueExpression(expression, parseResult, expectedType);
    }

    @Override
    public ValueExpression createValueExpression(Object instance, Class<?> expectedType) {
        Objects.requireNonNull(expectedType);
        return null;
    }
}