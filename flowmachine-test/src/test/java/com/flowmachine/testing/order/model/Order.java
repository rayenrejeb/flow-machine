package com.flowmachine.testing.order.model;

/**
 * Order context class for testing.
 */
public class Order {

  private String id;
  private double amount;
  private boolean paid;
  private boolean shipped;

  public Order(String id, double amount) {
    this.id = id;
    this.amount = amount;
  }

  public String getId() {
    return id;
  }

  public double getAmount() {
    return amount;
  }

  public boolean isPaid() {
    return paid;
  }

  public void setPaid(boolean paid) {
    this.paid = paid;
  }

  public boolean isShipped() {
    return shipped;
  }

  public void setShipped(boolean shipped) {
    this.shipped = shipped;
  }

  @Override
  public String toString() {
    return String.format("Order{id='%s', amount=%s, paid=%s, shipped=%s}",
        id, amount, paid, shipped);
  }
}