package cf.edof.lab.cms.menu;

import cf.edof.lab.cms.model.Product;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrudController {

    private  static final Logger logger = LoggerFactory.getLogger(CrudController.class);

    private final RoutingContext context;
    private final SQLClient sqlClient;

    public CrudController(RoutingContext context, SQLClient sqlClient) {
        this.context = context;
        this.sqlClient = sqlClient;
    }

    public void addProduct(){
        JsonObject val = context.getBody().toJsonObject();
        //System.out.println("CrudControler = "+val.encodePrettily());

        Product product = new Product();
        product.setProductName(val.getString("productName"));
        product.setPrice(val.getInteger("price"));
        product.setCondition(val.getString("condition"));
        product.setQuantity(val.getInteger("quantity"));
        product.setTotalCost(val.getInteger("totalCost"));

        //System.out.println("Product To save = "+product.toJson());

        JsonArray jsonArray = new JsonArray()
                .add(product.getProductName())
                .add(product.getPrice())
                .add(product.getCondition())
                .add(product.getQuantity())
                .add(product.getTotalCost());

        logger.debug("Product To Save = "+ product.toJson());

        sqlClient.updateWithParams("INSERT INTO product (product_name, price, condition, quantity, total_cost) "+
                "VALUES(?, ?, ?, ?, ?)", jsonArray, ar ->{
           if (ar.succeeded()){
               context.response()
                       .setStatusCode(201)
                       .putHeader("content-type","text/html")
                       .end(new JsonObject()
                               .put("message","successfully save product")
                               .put("product", product.toJson()).encodePrettily());
           }else{
               context.fail(ar.cause());
           }
        });
    }

    public void getProduct(){
        sqlClient.query("SELECT id_product as idProduct, product_name as productName, price, condition, quantity, total_cost as totalCost FROM product",ar ->{
           if (ar.succeeded()){
               int code = 400;
               JsonObject message = new JsonObject()
                       .put("message","not found")
                       .put("code",code);
               if (ar.result().getNumRows() != 0){
                   code = 200;
                   message
                           .put("message","success")
                           .put("code",code)
                           .put("product",ar.result().getRows());
               }
               context.response()
                       .setStatusCode(code)
                       .putHeader("content-type","application/json")
                       .end(message.encodePrettily());

           }else {
               context.fail(ar.cause());
           }
        });
    }

    public void getProductId(){
        JsonObject valObj = context.getBodyAsJson();
        sqlClient.query("SELECT id_product as idproduct, product_name as productName, price, condition, quantity, total_cost as totalCost FROM product "+
                "WHERE id_product = "+valObj.getInteger("idProduct"),ar ->{
            if (ar.succeeded()){
                int code = 400;
                JsonObject message = new JsonObject()
                        .put("message","Product Not Found!")
                        .put("code", code);

                if (ar.result().getNumRows() != 0){
                    code = 200;
                    message.put("message","success")
                            .put("code",code)
                            .put("product", ar.result().getRows().get(0));
                }

                context.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(code)
                        .end(message.encodePrettily());
            }else{
                context.fail(ar.cause());
            }
        });
    }

    public void editProduct(String id){
        Integer productId = Integer.parseInt(id);
        JsonObject val = context.getBody().toJsonObject();

        //System.out.println(val.encodePrettily());

        Product product = new Product();
        product.setProductName(val.getString("productName"));
        product.setPrice(val.getInteger("price"));
        product.setCondition(val.getString("condition"));
        product.setQuantity(val.getInteger("quantity"));
        product.setTotalCost(val.getInteger("totalCost"));

        JsonArray jsonArray = new JsonArray()
                .add(product.getProductName())
                .add(product.getPrice())
                .add(product.getCondition())
                .add(product.getQuantity())
                .add(product.getTotalCost())
                .add(productId);

        //System.out.println("JsonArray = "+jsonArray);

        logger.debug("Product To Update = "+product.toJson());
        //System.out.println("Product To Update = "+product.toJson());

        sqlClient.updateWithParams("UPDATE product SET product_name = ?, price = ?, condition = ?, quantity = ?, total_cost = ? "+
                "WHERE id_product = ?", jsonArray, ar->{
            if (ar.succeeded()){
                context.response()
                        .setStatusCode(200)
                        .putHeader("content-type","application/json")
                        .end(new JsonObject()
                                .put("message","success")
                                .put("code",200)
                                .put("product",product.toJson()).encodePrettily());
            }else{
                context.fail(ar.cause());
            }
        });
    }

    public void deleteProduct(){
        Integer id = Integer.parseInt(context.request().getParam("id"));

        logger.debug("Product to Delete(ID) = "+id);
        //System.out.println("productId : "+id);
        sqlClient.query("DELETE FROM product WHERE id_product="+id, ar ->{
           if (ar.succeeded()){
               context.response()
                       .putHeader("content-type", "application/json")
                       .setStatusCode(200)
                       .end(new JsonObject()
                               .put("code", 200)
                               .put("message","success").encodePrettily());
           }else{
               context.fail(ar.cause());
           }
        });
    }
}
