package com.food.project.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class Validator<T> {
    private Map<Predicate<T>, RuntimeException> filters = new HashMap<>();

    protected abstract void initializeValidations();

    protected void ruleFor(Predicate<T> filter, RuntimeException e) {
        filters.put(filter, e);
    }

    public void validate(T obj) throws RuntimeException {
        for (var entry : filters.entrySet()) {
            if (!entry.getKey().test(obj)) {
                throw entry.getValue();
            }
        }
    }
}
