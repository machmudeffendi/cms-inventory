package cf.edof.lab.cms.menu;

import cf.edof.lab.cms.model.UserLab;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final RoutingContext context;
    private final SQLClient sqlClient;

    public AccountController(RoutingContext context, SQLClient sqlClient){
        this.context = context;
        this.sqlClient = sqlClient;
    }

    public void routeLogin(){
        JsonObject val = new JsonObject(context.getBody());
        String username = val.getString("username");
        String password = val.getString("password");
        sqlClient.query("SELECT *  FROM user_lab WHERE username = '"+username+"' AND password = '"+password+"'", ar ->{
           if (ar.succeeded()){
               int code = 401;
               JsonObject message = new JsonObject()
                       .put("message","Unautorized!");
               if (ar.result().getNumRows() != 0){
                   code = 200;
                   message = new JsonObject()
                           .put("message","Success")
                           .put("account",ar.result().getRows().get(0));
               }
               context.response()
                       .setStatusCode(code)
                       .putHeader("content-type","application/json; charset=utf-8")
                       .end(message.encodePrettily());
           }else{
               context.fail(ar.cause());
           }
        });
        /*
        JsonObject val = new JsonObject(context.getBody());
        JsonObject registredUser = new JsonObject()
                .put("username","admin")
                .put("password","password");

        //System.out.println(registredUser.equals(val));
        int code = 401;
        JsonObject message = new JsonObject()
                .put("message","Unauthorized");
        if (registredUser.equals(val)){
            code = 200;
            message.put("message","successed")
                    .put("account",registredUser);
        }

        context.response()
                .setStatusCode(code)
                .putHeader("content-type","application/json; charset=utf-8")
                .end(message.encodePrettily());
        */
    }

    public void saveUser(){
        JsonObject val = context.getBodyAsJson();
        UserLab userLab = new UserLab();
        userLab.setName(val.getString("name"));
        userLab.setEmail(val.getString("email"));
        userLab.setUsername(val.getString("username"));
        userLab.setPassword(val.getString("password"));

        JsonArray jsonArray = new JsonArray()
                .add(userLab.getName())
                .add(userLab.getEmail())
                .add(userLab.getUsername())
                .add(userLab.getPassword());

        logger.debug("Account To Save = "+userLab.toJson());
        sqlClient.updateWithParams("INSERT INTO user_lab (name, email, username, password)"+
                "VALUES(?, ?, ?, ?)", jsonArray, ar ->{
            if (ar.succeeded()){
                context.response()
                        .setStatusCode(201)
                        .putHeader("content-type","application/json")
                        .end(new JsonObject()
                                .put("message","successfully save account")
                                .put("account",userLab.toJson()).encodePrettily());
            }else{
                context.fail(ar.cause());
            }
        });

    }
}
