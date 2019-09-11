package cf.edof.lab.cms.model;

import io.vertx.core.json.JsonObject;

public class Product {

    private String productName;
    private Integer price;
    private String condition;
    private Integer quantity;
    private Integer totalCost;

    public Product() {
    }

    public Product(String productName, Integer price, String condition, Integer quantity, Integer totalCost) {
        this.productName = productName;
        this.price = price;
        this.condition = condition;
        this.quantity = quantity;
        this.totalCost = totalCost;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Integer totalCost) {
        this.totalCost = totalCost;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject()
                .put("productName",productName)
                .put("price",price)
                .put("condition",condition)
                .put("quantity",quantity)
                .put("totalCost",totalCost);
        return jsonObject;
    }
}
