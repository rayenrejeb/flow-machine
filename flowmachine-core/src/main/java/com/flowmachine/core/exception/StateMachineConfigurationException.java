package com.flowmachine.core.exception;

public class StateMachineConfigurationException extends StateMachineException {

  public StateMachineConfigurationException(String message) {
    super(message);
  }

  public StateMachineConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}