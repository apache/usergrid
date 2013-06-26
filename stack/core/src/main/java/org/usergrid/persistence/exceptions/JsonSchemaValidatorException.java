package org.usergrid.persistence.exceptions;

import com.github.fge.jsonschema.report.ProcessingReport;

public class JsonSchemaValidatorException extends PersistenceException {

    private static final long serialVersionUID = 1L;

    final ProcessingReport report;

    public JsonSchemaValidatorException(ProcessingReport report) {
        super();
        this.report = report;
    }

    public JsonSchemaValidatorException(String message, Throwable cause, ProcessingReport report) {
        super(message, cause);
        this.report = report;
    }

    public JsonSchemaValidatorException(String message, ProcessingReport report) {
        super(message);
        this.report = report;
    }

    public JsonSchemaValidatorException(Throwable cause, ProcessingReport report) {
        super(cause);
        this.report = report;
    }

    public ProcessingReport getProcessingReport() {
        return report;
    }
}