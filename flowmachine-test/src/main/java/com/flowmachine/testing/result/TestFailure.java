package com.flowmachine.testing.result;

public record TestFailure(String message, Exception exception) {

  public TestFailure(String message) {
    this(message, null);
  }

}
