package cf.edof.lab.cms.model;

import io.vertx.core.json.JsonObject;

public class AllProduct {
    private Integer idProduct;
    private String productName;
    private Integer price;
    private String condition;
    private Integer quantity;
    private Integer totalCost;

    public AllProduct() {
    }

    public AllProduct(JsonObject jsonObject) {
        this.idProduct = jsonObject.getInteger("idProduct");
        this.productName = jsonObject.getString("productName");
        this.price = jsonObject.getInteger("price");
        this.condition = jsonObject.getString("condition");
        this.quantity = jsonObject.getInteger("quantity");
        this.totalCost = jsonObject.getInteger("totalCost");
    }

    public AllProduct(String productName, Integer price, String condition, Integer quantity, Integer totalCost) {
        this.productName = productName;
        this.price = price;
        this.condition = condition;
        this.quantity = quantity;
        this.totalCost = totalCost;
    }

    public Integer getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(Integer idProduct) {
        this.idProduct = idProduct;
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
                .put("idProduct",idProduct)
                .put("productName",productName)
                .put("price",price)
                .put("condition",condition)
                .put("quantity",quantity)
                .put("totalCost",totalCost);
        return jsonObject;
    }
}
