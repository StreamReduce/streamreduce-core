package com.streamreduce.rest.resource;

public final class ErrorMessage {

    public String errorMessage;

    public ErrorMessage() { /* Here for Jackson */ }

    public ErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}