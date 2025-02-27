package com.food.project.validator;

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
        System.out.println("Validation quantity: " + filters.entrySet().size());
        for (var entry : filters.entrySet()) {
            System.out.println("Validating " + obj);
            if (!entry.getKey().test(obj)) {
                System.out.println("Throwing exception due to validation check error");
                throw entry.getValue();
            }
            System.out.println("Validated check " + entry.getValue());
        }
    }
}
