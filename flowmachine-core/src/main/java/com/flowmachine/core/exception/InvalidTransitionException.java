package com.flowmachine.core.exception;

public class InvalidTransitionException extends StateMachineException {

  public InvalidTransitionException(String message) {
    super(message);
  }

  public InvalidTransitionException(String message, Throwable cause) {
    super(message, cause);
  }
}