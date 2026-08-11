package com.khesam.dezhban.dataaccess.local.converter;

import com.khesam.dezhban.common.GrantType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class GrantTypeConverter implements AttributeConverter<Set<GrantType>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<GrantType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<GrantType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashSet<>();
        }

        return  Arrays.stream(dbData.split(DELIMITER))
                .map(GrantType::valueOf)
                .collect(Collectors.toSet());
    }
}
