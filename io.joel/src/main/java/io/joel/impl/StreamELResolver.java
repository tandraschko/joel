package io.joel.impl;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;

import java.beans.FeatureDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class StreamELResolver extends ELResolver {
    private static final MethodHandle CONVERT_TO_TYPE;
    private static final MethodHandle LAMBDA_INVOKE;

    static {
        try {
            CONVERT_TO_TYPE = MethodHandles.lookup().findVirtual(ELContext.class, "convertToType", MethodType.methodType(Object.class, Object.class, Class.class));
            LAMBDA_INVOKE = MethodHandles.lookup().findVirtual(LambdaExpression.class, "invoke", MethodType.methodType(Object.class, ELContext.class, Object[].class));
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return String.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    @Override
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] parameterTypes, Object[] params) {
        Objects.requireNonNull(context);
        if (!(base instanceof Stream<?>))
            return null;
        try {
            String methodName = (String) method;

            if ("substream".equals(methodName)) {
                base = ((Stream<?>) base).skip(((long) params[0]));
                if (params.length == 2) {
                    base = ((Stream<?>) base).limit((long) params[1] - (long) params[0]);
                }
                context.setPropertyResolved(base, method);
                return base;
            }

            if ("sorted".equals(methodName) || "min".equals(methodName) || "max".equals(methodName)) {
                if (params == null || params.length == 0) {
                    params = new Object[]{Comparator.naturalOrder()};
                }
            }

            Class<?> streamClass = Stream.class;
            if ("average".equals(methodName) || "sum".equals(methodName)) {
                base = ((Stream<?>) base).mapToLong(x -> context.convertToType(x, Long.class));
                streamClass = LongStream.class;
            }
            Object[] currentParams = params;
            var method1 = Arrays.stream(streamClass.getMethods())
                    .filter(x -> !Modifier.isStatic(x.getModifiers()))
                    .filter(x -> x.getName().equals(methodName))
                    .filter(x -> parameterTypes == null || Arrays.equals(parameterTypes, x.getParameterTypes()))
                    .filter(x -> currentParams == null || currentParams.length == x.getParameterCount())
                    .findFirst()
                    .orElseThrow(NoSuchMethodException::new);

            // toList() / toArray()
            if (method1.getParameterCount() == 0) {
                context.setPropertyResolved(base, method);
                return method1.invoke(base);
            }

            // sorted() / min() / max()
            if (params != null && params.length != 0 && !(params[0] instanceof LambdaExpression)) {
                context.setPropertyResolved(base, method);
                return method1.invoke(base, params);
            }

            LambdaExpression lambdaParam = (LambdaExpression) params[0];

            // parameters of stream method
            Class<?>[] parameterTypes1 = method1.getParameterTypes();
            if (parameterTypes1.length > 1)
                throw new ELException("Handle multiple stream arguments");

            Class<?> aClass = parameterTypes1[0];
            if (aClass.isPrimitive())
                throw new ELException("Handle primitive arguments");

            List<String> equals = List.of("equals", "hashCode", "toString");

            Method method2 = Arrays.stream(aClass.getMethods())
                    .filter(x -> !x.isDefault())
                    .filter(x -> !Modifier.isStatic(x.getModifiers()))
                    .filter(x -> equals.stream().noneMatch(x.getName()::equals))
                    .findFirst()
                    .orElseThrow(NoSuchMethodException::new);
            Class<?> returnType = method2.getReturnType();


            // lambdaParam.invoke(context, ?)
            MethodHandle methodHandle = MethodHandles.insertArguments(LAMBDA_INVOKE, 0, lambdaParam, context);

            // context.convertToType(?, ?)
            MethodHandle methodHandle2 = CONVERT_TO_TYPE.bindTo(context);

            // context.convertToType(?, returnType)
            MethodHandle methodHandle3 = MethodHandles.insertArguments(methodHandle2, 1, returnType);

            // context.convertToType(lambdaParam.invoke(context, ?), returnType)
            MethodHandle methodHandle4 = MethodHandles.filterArguments(methodHandle3, 0, methodHandle);

            MethodHandle methodHandle1 = methodHandle4
                    // context.convertToType(lambdaParam.invoke(context, Object...?), returnType)
                    .asType(MethodType.methodType(returnType, Object[].class)).withVarargs(true)
                    .asType(MethodType.methodType(returnType, method2.getParameterTypes()));

            Object proxy = MethodHandleProxies.asInterfaceInstance(aClass, methodHandle1);
            context.setPropertyResolved(base, method);
            return method1.invoke(base, proxy);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new MethodNotFoundException(noSuchMethodException);
        } catch (Throwable throwable) {
            throw new ELException(throwable);
        }
    }
}