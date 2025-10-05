package com.flowmachine.testing.shopping.model;

/**
 * Shopping cart context class for testing.
 */
public class ShoppingCart {

  private int itemCount = 0;
  private double totalAmount = 0.0;
  private boolean discountApplied = false;
  private String promotionCode;

  public void addItem(double price) {
    itemCount++;
    totalAmount += price;
  }

  public void applyDiscount(double percentage) {
    if (!discountApplied) {
      totalAmount = totalAmount * (1 - percentage / 100);
      discountApplied = true;
    }
  }

  public void setPromotionCode(String code) {
    this.promotionCode = code;
  }

  public int getItemCount() {
    return itemCount;
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public boolean isDiscountApplied() {
    return discountApplied;
  }

  public String getPromotionCode() {
    return promotionCode;
  }

  @Override
  public String toString() {
    return String.format("ShoppingCart{items=%d, total=%.2f, discount=%s, promo='%s'}",
        itemCount, totalAmount, discountApplied, promotionCode);
  }
}