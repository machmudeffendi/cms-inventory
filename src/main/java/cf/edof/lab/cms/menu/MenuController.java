package cf.edof.lab.cms.menu;

import cf.edof.lab.cms.model.AllProduct;
import cf.edof.lab.cms.service.AES;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.common.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    private final RoutingContext context;

    public MenuController(RoutingContext context){
        this.context = context;
    }

    public void routeDashboard(TemplateEngine templateEngine){
        JsonObject data = new JsonObject();
        templateEngine.render(data, "webroot/view/priv/dashboard.html",ar ->{
            if (ar.succeeded()){
                context.response()
                        .putHeader("Content-Type","text/html")
                        .end(ar.result());
            }else{
                context.fail(ar.cause());
            }
        });
    }

    public void routeProduct(TemplateEngine templateEngine, Vertx vertx){

        WebClient client = WebClient.create(vertx);

        client.get(8081,"localhost","/crud/getproduct")
                .putHeader("content-type","application/json")
                .send(ar ->{
                    if (ar.succeeded()){
                        JsonObject response = ar.result().bodyAsJsonObject();
                        //System.out.println(response.getValue("code").equals(200));
                        if (response.containsKey("code") && response.getValue("code").equals(200)){
                            List<AllProduct> allProducts = response.getJsonArray("product")
                                    .stream().map(s -> new AllProduct(JsonObject.mapFrom(s)))
                                    .collect(Collectors.toList());

                            Session session = context.session();

                            Cookie cookie = context.getCookie("remember-cookie");

                            if (session.get("adisca_acc") == null && cookie == null){
                                context.addCookie(Cookie.cookie("message","session_end"))
                                        .response()
                                        .putHeader("location","/")
                                        .setStatusCode(302)
                                        .end();
                                return;
                            }

                            JsonObject userData = session.get("adisca_acc") != null ? new JsonObject(AES.decrypt(session.get("adisca_acc"),"edof"))
                                    : new JsonObject(AES.decrypt(cookie.getValue(),"edof"));

                            JsonObject data = new JsonObject()
                                    .put("productList", allProducts)
                                    .put("loginUserData",userData)
                                    .put("username", userData.getString("username"));
                            //System.out.println(data.encodePrettily());

                            templateEngine.render(data, "webroot/view/priv/product.html", er ->{
                                if (er.succeeded()){
                                    context.response()
                                            .putHeader("content-type","text/html")
                                            .end(er.result());
                                }else{
                                    context.fail(er.cause());

                                }
                            });
                        }else{
                            JsonObject data = new JsonObject()
                                    .put("productList", (JsonArray) null);
                            System.out.println(data.encodePrettily());

                            templateEngine.render(data, "webroot/view/priv/product.html", er ->{
                                if (er.succeeded()){
                                    context.response()
                                            .putHeader("content-type","text/html")
                                            .end(er.result());
                                }else{
                                    context.fail(er.cause());

                                }
                            });
                        }
                    }else{
                        logger.debug(ar.cause().getMessage());
                    }
                });

    }

    public void saveProductContent(Vertx vertx){
        JsonObject productObj = new JsonObject()
                .put("productName",context.request().getParam("product_name"))
                .put("price",Integer.parseInt(context.request().getParam("price")))
                .put("condition",context.request().getParam("condition"))
                .put("quantity",Integer.parseInt(context.request().getParam("quantity")))
                .put("totalCost",Integer.parseInt(context.request().getParam("total_cost")));
        //System.out.println(productObj.encodePrettily());

        WebClient client = WebClient.create(vertx);

        client.post(8081,"localhost","/crud/addproduct")
                .putHeader("content-type","appilcation/json")
                .sendJsonObject(productObj, ar ->{
                    if (ar.succeeded()){
                        context.response()
                                .putHeader("location","/menu/product")
                                .setStatusCode(302)
                                .end();
                    }else{
                        context.fail(ar.cause());
                    }
                });

    }

    public void updateProductForm(TemplateEngine templateEngine, Vertx vertx ,String id){
        Integer idproduct = Integer.parseInt(id);

        WebClient client = WebClient.create(vertx);

        client.get(8081,"localhost", "/crud/getproductid")
                .sendJsonObject(new JsonObject()
                        .put("idProduct",idproduct),ar ->{
                if (ar.succeeded()){
                    JsonObject response = ar.result().bodyAsJsonObject();
                    //System.out.println(response.getJsonObject("product"));
                    if (response.containsKey("code") && response.getValue("code").equals(200)){
                        JsonObject productObj = response.getJsonObject("product");

                        AllProduct allProduct = new AllProduct();
                        allProduct.setIdProduct(productObj.getInteger("idproduct"));
                        allProduct.setProductName(productObj.getString("productname"));
                        allProduct.setPrice(productObj.getInteger("price"));
                        allProduct.setCondition(productObj.getString("condition"));
                        allProduct.setQuantity(productObj.getInteger("quantity"));
                        allProduct.setTotalCost(productObj.getInteger("totalcost"));

                        Session session = context.session();

                        Cookie cookie = context.getCookie("remember-cookie");

                        if (session.get("adisca_acc") == null && cookie == null){
                            context.addCookie(Cookie.cookie("message", "session_end"))
                                    .response()
                                    .setStatusCode(302)
                                    .putHeader("location", "/")
                                    .end();
                        }

                        JsonObject userData = session.get("adisca_acc") != null ? new JsonObject(AES.decrypt(session.get("adisca_acc"),"edof"))
                                : new JsonObject(AES.decrypt(cookie.getValue(), "edof"));

                        JsonObject data = allProduct.toJson()
                                .put("loginUserData", userData)
                                .put("username", userData.getString("username"));
                        System.out.println(data.encodePrettily());

                        templateEngine.render(data, "webroot/view/priv/update-product.html", er ->{
                            if (er.succeeded()){
                                context.response()
                                        .putHeader("content-type", "text/html")
                                        .end(er.result());
                            }else{
                                context.fail(er.cause());
                            }
                        });
                    }
                }else{
                    logger.debug(ar.cause().getMessage());
                }
        });

    }

    public void saveUpdateProduct(Vertx vertx){
        JsonObject productObj = new JsonObject()
                .put("idProduct", Integer.parseInt(context.request().getParam("id_product")))
                .put("productName",context.request().getParam("product_name"))
                .put("price", Integer.parseInt(context.request().getParam("price")))
                .put("condition",context.request().getParam("condition"))
                .put("quantity", Integer.parseInt(context.request().getParam("quantity")))
                .put("totalCost", Integer.parseInt(context.request().getParam("total_cost")));

        WebClient client = WebClient.create(vertx);

        client.put(8081,"localhost","/crud/editproduct/"+productObj.getInteger("idProduct"))
                .sendJsonObject(productObj, ar ->{
                    if (ar.succeeded()){
                        context.response()
                                .putHeader("location", "/menu/product")
                                .setStatusCode(302)
                                .end();
                    }else{
                        context.fail(ar.cause());
                    }
                });
    }

    public void deleteProduct(Vertx vertx){
        Integer id = Integer.parseInt(context.request().getParam("id"));
        System.out.println(id);
        WebClient client = WebClient.create(vertx);

        client.delete(8081, "localhost", "/crud/deleteproduct/"+id)
                .send(ar ->{
                    JsonObject response = ar.result().bodyAsJsonObject();
                    if(ar.succeeded()){
                        if (response.containsKey("code") && response.getValue("code").equals(200)){
                            context.response()
                                    .setStatusCode(302)
                                    .putHeader("location", "/menu/product")
                                    .end();
                        }
                    }else{
                        context.fail(ar.cause());
                    }
                });
    }

    public void routeFormInsertProduct(TemplateEngine templateEngine){
        JsonObject data = new JsonObject();

        Session session = context.session();

        Cookie cookie = context.getCookie("remember-cookie");

        if (session.get("adisca_acc") == null && cookie == null){
            context.addCookie(Cookie.cookie("message","session_end"))
                    .response()
                    .putHeader("location","/")
                    .setStatusCode(302)
                    .end();
            return;
        }

        templateEngine.render(data, "webroot/view/priv/insert-product.html", ar ->{
            if (ar.succeeded()){
                context.response()
                        .putHeader("content-type", "text/html")
                        .end(ar.result());
            }else{
                context.fail(ar.cause());
            }
        });
    }
}
