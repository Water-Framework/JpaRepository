/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.repository.jpa.query;

import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.operands.FieldNameOperand;
import it.water.core.api.repository.query.operands.FieldValueOperand;
import it.water.core.api.repository.query.operations.*;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class PredicateBuilder<T> {
    private Root<T> entityDef;
    private CriteriaQuery<?> cq;
    private CriteriaBuilder cb;

    public Predicate buildPredicate(Query filter) {
        if (filter instanceof AndOperation andOperation) {
            return cb.and(this.buildPredicate(andOperation.getOperand(0)), this.buildPredicate(andOperation.getOperand(1)));
        } else if (filter instanceof OrOperation orOperation) {
            return cb.or(this.buildPredicate(orOperation.getOperand(0)), this.buildPredicate(orOperation.getOperand(1)));
        } else if (filter instanceof NotOperation notOperation) {
            return cb.not(this.buildPredicate(notOperation.getOperand(0)));
        } else if (filter instanceof BinaryValueOperation binaryValueOperation) {
            Path p = getPathForFields((AbstractOperation) filter);
            FieldValueOperand fieldValue = (FieldValueOperand) binaryValueOperation.getOperand(1);
            if (filter instanceof EqualTo) {
                return cb.equal(p, convertToEntityFieldType(p.getJavaType(),fieldValue.getValue()));
            } else if (filter instanceof NotEqualTo) {
                return cb.notEqual(p, fieldValue.getValue());
            } else if (filter instanceof GreaterOrEqualThan) {
                Double d = Double.parseDouble(fieldValue.getValue().toString());
                return cb.greaterThanOrEqualTo(p, d);
            } else if (filter instanceof GreaterThan) {
                Double d = Double.parseDouble(fieldValue.getValue().toString());
                return cb.greaterThan(p, d);
            } else if (filter instanceof LowerOrEqualThan) {
                Double d = Double.parseDouble(fieldValue.getValue().toString());
                return cb.lessThanOrEqualTo(p, d);
            } else if (filter instanceof LowerThan) {
                Double d = Double.parseDouble(fieldValue.getValue().toString());
                return cb.lessThan(p, d);
            } else if (filter instanceof Like) {
                return cb.like(p, fieldValue.getDefinition());
            }
        } else if (filter instanceof BinaryValueListOperation) {
            Path<?> p = getPathForFields((AbstractOperation) filter);
            if (filter instanceof In in) {
                List<Object> values = getValueList(in);
                return p.in(values);
            }
        }
        throw new UnsupportedOperationException("Invalid operation");
    }

    private Object convertToEntityFieldType(Class<?> type, Object value) {
        if (value == null || type == null) return null;
        String valueStr = value.toString();
        if (type.equals(String.class)) {
            return value;
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.valueOf(valueStr);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.valueOf(valueStr);
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return Boolean.valueOf(valueStr);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return Double.valueOf(valueStr);
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            return Float.valueOf(valueStr);
        }

        throw new IllegalArgumentException("Not supported type: " + type.getName());
    }

    /**
     * Returns path for first operand of every operation.
     * This is because every standard operation should be in the form <field> <Operator> <value/values>
     *
     * @param operation
     * @return
     */
    protected Path<Object> getPathForFields(AbstractOperation operation) {
        String fieldPath = operation.getOperand(0).getDefinition();
        Path<?> rootPath = null;
        int periodIndex = fieldPath.indexOf(".");
        if (periodIndex > 0) {
            String rootFieldPath = (fieldPath.split("\\."))[0];
            rootPath = entityDef.get(rootFieldPath);
            return getFieldPath(rootPath, fieldPath.substring(periodIndex + 1));
        } else {
            return entityDef.get(fieldPath);
        }
    }

    /**
     * Return specific path object given a string representation of a field path
     *
     * @param rootPath
     * @param fieldPath
     * @return
     */
    private Path<Object> getFieldPath(Path<?> rootPath, String fieldPath) {
        int periodIndex = fieldPath.indexOf(".");
        if (periodIndex > 0) {
            String field = (fieldPath.split("\\."))[0];
            rootPath = rootPath.get(field);
            return getFieldPath(rootPath, fieldPath.substring(periodIndex + 1));
        } else {
            return rootPath.get(fieldPath);
        }
    }

    /**
     * Returns values list Object defined inside the query ex. age IN (10,11,12)
     * it returns a List {10,11,12}
     *
     * @param binaryValueListOperation
     * @return
     */
    public List<Object> getValueList(BinaryValueListOperation binaryValueListOperation) {
        final List<Object> operandValues = new ArrayList<>();
        final AtomicReference<FieldNameOperand> field = new AtomicReference<>();
        binaryValueListOperation.operands().stream().forEach(operand -> {
            boolean isOperand = true;
            if ((operand instanceof FieldNameOperand fieldNameOperand) && field.get() == null) {
                field.set(fieldNameOperand);
                isOperand = false;
            } else if (!(operand instanceof FieldValueOperand) || ((operand instanceof FieldNameOperand) && field.get() != null)) {
                throw new IllegalArgumentException("Invalid argument for expression, value needed");
            }
            if (isOperand)
                operandValues.add(((FieldValueOperand) operand).getValue());
        });
        return operandValues;
    }
}
