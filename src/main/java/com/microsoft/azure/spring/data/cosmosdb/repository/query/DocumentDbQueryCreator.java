/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link DocumentDbQueryCreator} is deprecated.
 * Instead use CosmosQueryCreator, which is introduced in 2.2.0 version.
 */
@Deprecated
public class DocumentDbQueryCreator extends AbstractQueryCreator<DocumentQuery, Criteria> {

    private final MappingContext<?, DocumentDbPersistentProperty> mappingContext;

    public DocumentDbQueryCreator(PartTree tree, DocumentDbParameterAccessor accessor,
                                  MappingContext<?, DocumentDbPersistentProperty> mappingContext) {
        super(tree, accessor);

        this.mappingContext = mappingContext;
    }

    private String getSubject(@NonNull Part part) {
        String subject = mappingContext.getPersistentPropertyPath(part.getProperty()).toDotPath();
        final Class<?> domainClass = part.getProperty().getOwningType().getType();

        @SuppressWarnings("unchecked") final DocumentDbEntityInformation information =
                new DocumentDbEntityInformation(domainClass);

        if (information.getIdField().getName().equals(subject)) {
            subject = Constants.ID_PROPERTY_NAME;
        }

        return subject;
    }

    @Override // Note (panli): side effect here, this method will change the iterator status of parameters.
    protected Criteria create(Part part, Iterator<Object> parameters) {
        final Part.Type type = part.getType();
        final String subject = getSubject(part);
        final List<Object> values = new ArrayList<>();

        if (CriteriaType.isPartTypeUnSupported(type)) {
            throw new UnsupportedOperationException("Unsupported keyword: " + type);
        }

        for (int i = 0; i < part.getNumberOfArguments(); i++) {
            Assert.isTrue(parameters.hasNext(), "should not reach the end of iterator");
            values.add(parameters.next());
        }

        return Criteria.getInstance(CriteriaType.toCriteriaType(type), subject, values);
    }

    @Override
    protected Criteria and(@NonNull Part part, @NonNull Criteria base, @NonNull Iterator<Object> parameters) {
        final Criteria right = this.create(part, parameters);

        return Criteria.getInstance(CriteriaType.AND, base, right);
    }

    @Override
    protected Criteria or(@NonNull Criteria base, @NonNull Criteria criteria) {
        return Criteria.getInstance(CriteriaType.OR, base, criteria);
    }

    @Override
    protected DocumentQuery complete(@NonNull Criteria criteria, @NonNull Sort sort) {
        return new DocumentQuery(criteria).with(sort);
    }
}
