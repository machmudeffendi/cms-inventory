package cf.edof.lab.cms.menu;

import cf.edof.lab.cms.model.Product;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.RoutingContext;
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

           }
        });
    }
}
