package com.yamiapp.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class Validator<T> {
    private Map<Predicate<T>, RuntimeException> filters;

    protected abstract void initializeValidations();

    protected Validator() {
        this.filters = new HashMap<>();
        initializeValidations();
    }

    protected void ruleFor(Predicate<T> filter, RuntimeException e) {
        filters.put(filter, e);
    }

    public void validate(T obj) throws RuntimeException {
        for (var entry : filters.entrySet()) {
            if (!entry.getKey().test(obj)) {
                System.out.println("Throwing exception due to validation check error");
                throw entry.getValue();
            }
        }
    }
}
