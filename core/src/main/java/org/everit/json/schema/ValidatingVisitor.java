package org.everit.json.schema;

import static org.everit.json.schema.EnumSchema.toJavaValue;

import org.json.JSONObject;

class ValidatingVisitor extends Visitor {

    protected Object subject;

    protected FailureReporter failureReporter;

    ValidatingVisitor(Object subject, Schema schema) {
        this.subject = subject;
        this.failureReporter = new FailureReporter(schema);
    }

    @Override void visitNumberSchema(NumberSchema numberSchema) {
        numberSchema.accept(new NumberSchemaValidatingVisitor(subject, failureReporter));
    }

    @Override void visitArraySchema(ArraySchema arraySchema) {
        arraySchema.accept(new ArraySchemaValidatingVisitor(subject, this, failureReporter));
    }

    @Override void visitBooleanSchema(BooleanSchema schema) {
        if (!(subject instanceof Boolean)) {
            failureReporter.failure(Boolean.class, subject);
        }
    }

    @Override void visitNullSchema(NullSchema nullSchema) {
        if (!(subject == null || subject == JSONObject.NULL)) {
            failureReporter.failure("expected: null, found: " + subject.getClass().getSimpleName(), "type");
        }
    }

    @Override void visitConstSchema(ConstSchema constSchema) {
        if (isNull(subject) && isNull(constSchema.getPermittedValue())) {
            return;
        }
        Object effectiveSubject = toJavaValue(subject);
        if (!ObjectComparator.deepEquals(effectiveSubject, constSchema.getPermittedValue())) {
            failureReporter.failure("", "const");
        }
    }

    private boolean isNull(Object obj) {
        return obj == null || JSONObject.NULL.equals(obj);
    }

    ValidationException getFailureOfSchema(Schema schema, Object input) {
        Object origSubject = this.subject;
        this.subject = input;
        return failureReporter.inContextOfSchema(schema, () -> {
            visit(schema);
            this.subject = origSubject;
        });
    }

    void failIfErrorFound() {
        failureReporter.throwExceptionIfFailureFound();
    }

}
