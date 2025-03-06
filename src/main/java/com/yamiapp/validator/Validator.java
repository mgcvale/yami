package com.yamiapp.validator;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class Validator<T> {
    private Map<Predicate<T>, RuntimeException> filters;

    protected abstract void initializeValidations();

    protected Validator() {
        this.filters = new LinkedHashMap<>();
        initializeValidations();
    }

    protected void ruleFor(Predicate<T> filter, RuntimeException e) {
        filters.put(filter, e);
    }

    public void validate(T obj) throws RuntimeException {
        if (obj == null) {
            throw new BadRequestException(ErrorStrings.EMPTY_FIELDS_NULL_POINTER.getMessage());
        }
        for (var entry : filters.entrySet()) {
            if (!entry.getKey().test(obj)) {
                System.out.println("Throwing exception due to validation check error");
                throw entry.getValue();
            }
        }
    }
}
