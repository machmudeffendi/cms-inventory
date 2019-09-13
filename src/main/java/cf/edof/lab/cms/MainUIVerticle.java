package cf.edof.lab.cms;

import cf.edof.lab.cms.menu.AccountController;
import cf.edof.lab.cms.menu.CrudController;
import cf.edof.lab.cms.menu.MenuController;
import cf.edof.lab.cms.service.AES;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainUIVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainUIVerticle.class);
    private ThymeleafTemplateEngine templateEngine;
    private AuthProvider authProvider;
    private SQLClient postgreSQLClient;

    public void start(Future<Void> startFuture) throws  Exception{

        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BodyHandler.create());
        router.route().handler(StaticHandler.create());

        router.get("/").handler(this::indexHandler);
        router.post("/login").handler(this::pageLoginHandler);
        router.get("/logout").handler(this::logout);

        router.get("/register").handler(this::pageRegisterHandler);
        router.post("/signup").handler(this::signupHandler);

        //Menu EndPoint
        router.get("/menu/dashboard").handler(this::menuController);
        router.get("/menu/product").handler(this::menuController);
        router.post("/menu/save-product").handler(this::menuController);
        router.get("/menu/update-product/:id").handler(this::menuController);
        router.post("/menu/update-product").handler(this::menuController);

        //Auth EndPoint API
        router.post("/account/auth").handler(this::accountController);
        router.post("/account/register").handler(this::accountController);

        //CRUD EndPint API
        router.post("/crud/addproduct").handler(this::crudController);
        router.get("/crud/getproduct").handler(this::crudController);
        router.get("/crud/getproductid").handler(this::crudController);
        router.put("/crud/editproduct/:id").handler(this::crudController);

        //Get app service config
        JsonObject appConfig = config().getJsonObject("app.service");
        String host = appConfig.getString("web.ui.http.address", "localhost");
        Integer port = appConfig.getInteger("web.ui.http.port",8080);

        //Get database config
        /*
        JsonObject dbConfig = config().getJsonObject("database");
        String dbHost = dbConfig.getString("host");
        Integer dbPort = dbConfig.getInteger("port");
        String dbDatabase = dbConfig.getString("database");
        String dbUsername = dbConfig.getString("username");
        String Password = dbConfig.getString("password");
        */

        // To create a PostgreSQL client:

        JsonObject postgreSQLClientConfig = config().getJsonObject("database");
        this.postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);

        templateEngine = ThymeleafTemplateEngine.create(vertx);
        templateEngine.getThymeleafTemplateEngine().addDialect(new LayoutDialect());

        server
                .requestHandler(router)
                .listen(port, host, ar ->{
                   if (ar.succeeded()){
                       System.out.println("Server Start On "+host+" Port "+port);
                       startFuture.complete();
                   }else{
                       System.out.println("Could not start a HTTP server\n"+ar.cause());
                       startFuture.fail(ar.cause());
                   }
                });

    }

    public void indexHandler(RoutingContext context){
        Cookie cookieLogin = context.getCookie("remember-cookie");
        if (cookieLogin != null){
            JsonObject cookieLoginData = new JsonObject(AES.decrypt(cookieLogin.getValue(), "edof"));
            this.enterDashboard(context, cookieLoginData);
        }else{
            Cookie cookie = context.getCookie("message");
            JsonObject obj = new JsonObject();
            logger.debug("COOKIE = "+ cookie);
            if (cookie != null){
                switch (cookie.getValue()){
                    case "error_credential":
                        obj.put("login","failed");
                        obj.put("message","unknown username/password");
                        break;
                    case "error_request":
                        obj.put("login","failed");
                        obj.put("message","error request");
                        break;
                    case "error_save":
                        obj.put("login","failed");
                        obj.put("message","can't save data");
                        break;
                    case "session_end":
                        obj.put("login","failed");
                        obj.put("message","Session already ended. Please login again");
                        break;
                    case "server_error":
                        obj.put("login","failed");
                        obj.put("message","Internal Server Error");
                        break;
                    case "confirmation_data_empty":
                        obj.put("login","failed");
                        obj.put("message","Can't Find Confirmation Data");
                        break;
                    case "success_forgot_password":
                        obj.put("login","failed");
                        obj.put("message","Successfully Update Password");
                        break;
                }
                cookie.setMaxAge(0);
            }

            logger.debug("Username context index = "+context.get("username"));
            Session session = context.session();
            if (session.get("adisca_acc") != null){
                JsonObject userSession = new JsonObject(AES.decrypt(session.get("adisca_acc"),"edof"));
                logger.debug("User session = "+userSession.toString());
                context.put("username",userSession.getString("username"));

                this.enterDashboard(context, userSession);
            }else{
                templateEngine.render(obj,"webroot/view/login.html",ar->{
                    if (ar.succeeded()){
                        context.response()
                                .putHeader("content-type","text/html")
                                .end(ar.result());
                    }else{
                        context.fail(ar.cause());
                    }
                });
            }
        }
    }

    private void pageLoginHandler(RoutingContext context){
        String username = context.request().getParam("username");
        String password = context.request().getParam("password");

        final String rememberMe = context.request().getParam("rememberne");

        WebClient client = WebClient.create(vertx);
        client.post(8081, "localhost", "/account/auth")
                .putHeader("content-type","application/json")
                .sendJsonObject(new JsonObject()
                        .put("username",username)
                        .put("password",password), ar ->{
                    logger.debug("Auth req = "+ar.result());
                    HttpResponse<Buffer> response = ar.result();
                    if (ar.succeeded()){
                        logger.debug("Auth req = "+response.statusCode());
                        if (response.statusCode() == 200){
                            //Add username to context
                            logger.debug("username context login = "+context.get("username"));
                            context.put("username", username);
                            context.session().put("username", username);

                            JsonObject accountResponse = response.bodyAsJsonObject();
                            JsonObject jsonObject = accountResponse.getJsonObject("account");
                            logger.debug("Account ==> "+jsonObject.encodePrettily());
                            jsonObject.put("login","success");

                            String objectEncripted = AES.encrypt(jsonObject.encodePrettily(), "edof");
                            templateEngine.render(jsonObject, "webroot/view/priv/dashboard.html", er ->{
                                if (er.succeeded()){
                                    if (rememberMe != null){
                                        context.addCookie(Cookie.cookie("remember-cookie", objectEncripted));
                                    }else{
                                        Session session = context.session();
                                        session.put("adisca_acc",objectEncripted);
                                    }

                                    context.response()
                                            .putHeader("content-type","text/html")
                                            .end(er.result());
                                }else{
                                    context.fail(er.cause());
                                }
                            });
                        }else{
                            context.addCookie(Cookie.cookie("message","error_credential"));
                            context.response()
                                    .putHeader("location","/")
                                    .setStatusCode(302)
                                    .end();
                        }
                    }else{
                        context.addCookie(Cookie.cookie("message","error_request"));
                        context.response()
                                .putHeader("location","/")
                                .setStatusCode(302)
                                .end();
                    }
                });
    }

    private void pageRegisterHandler(RoutingContext context){
        JsonObject request = new JsonObject();
        templateEngine.render(request, "webroot/view/register.html",ar->{
            if (ar.succeeded()){
                context.response()
                        .putHeader("content-type","text/html")
                        .end(ar.result());
            }else{
                context.fail(ar.cause());
            }
        });
    }

    private void signupHandler(RoutingContext context){
        String name = context.request().getParam("name");
        String email = context.request().getParam("email");
        String username = context.request().getParam("username");
        String password = context.request().getParam("password");

        WebClient client = WebClient.create(vertx);
        client.post(8081, "localhost", "/account/register")
                .putHeader("content-header","application/json")
                .sendJsonObject(new JsonObject()
                        .put("name",name)
                        .put("email",email)
                        .put("username",username)
                        .put("password",password), ar ->{
                    if (ar.succeeded()){
                        System.out.println("succeed");
                        if (ar.result().statusCode() == 201){
                            context.addCookie(Cookie.cookie("message","success"));
                            context.response()
                                    .putHeader("location","/")
                                    .setStatusCode(302)
                                    .end();
                        }else{
                            context.addCookie(Cookie.cookie("message","error_save"));
                            context.response()
                                    .putHeader("location","/")
                                    .setStatusCode(302)
                                    .end();
                        }
                    }else{
                        logger.info(ar.cause().getMessage());
                        context.addCookie(Cookie.cookie("message","error_request"));
                        context.response()
                                .putHeader("location","/")
                                .setStatusCode(302)
                                .end();
                    }
                });
    }

    private void enterDashboard(RoutingContext context, JsonObject data){
        String objectEncrypted = AES.encrypt(data.encodePrettily(),"edof");
        templateEngine.render(data, "webroot/view/priv/dashboard.html", ar ->{
            if (ar.succeeded()){
                Session session = context.session();
                session.put("adisca_acc",objectEncrypted);
                context.response()
                        .putHeader("content-type","text/html")
                        .end(ar.result());
            }else{
                context.fail(ar.cause());
            }
        });
    }

    private void logout(RoutingContext context){
        Session session = context.session();
        session.remove("adisca_acc");
        Cookie cookie = context.getCookie("remember-cookie");
        if (cookie != null){
            cookie.setMaxAge(0);
        }
        context.response()
                .putHeader("location","/")
                .setStatusCode(302)
                .end();
    }

    private void accountController(RoutingContext context){
        AccountController acc = new AccountController(context,postgreSQLClient);
        logger.debug("PATH = "+context.request().path());
        if ("/account/auth".equalsIgnoreCase(context.request().path())){
            acc.routeLogin();
        }else if ("/account/register".equalsIgnoreCase(context.request().path())){
            acc.saveUser();
        }
    }

    private void crudController(RoutingContext context){
        CrudController crud = new CrudController(context,postgreSQLClient);
        logger.debug("PATH = "+context.request().path());
        if ("/crud/addproduct".equalsIgnoreCase(context.request().path())){
            crud.addProduct();
        }else if ("/crud/getproduct".equalsIgnoreCase(context.request().path())){
            crud.getProduct();
        }else if (String.format("/crud/editproduct/%s", context.request().getParam("id")).equalsIgnoreCase(context.request().path())){
            String id = context.request().getParam("id");
            crud.editProduct(id);
        }else if ("/crud/getproductid".equalsIgnoreCase(context.request().path())){
            crud.getProductId();
        }
    }

    private void menuController(RoutingContext context){
        MenuController menu = new MenuController(context);
        CrudController crudController = new CrudController(context,postgreSQLClient);
        logger.debug("PATH = "+context.request().path());
        if ("/menu/dashboard".equalsIgnoreCase(context.request().path())){
            menu.routeDashboard(templateEngine);
        }else if("/menu/product".equalsIgnoreCase(context.request().path())){
            menu.routeProduct(templateEngine, vertx);
        }else if("/menu/save-product".equalsIgnoreCase(context.request().path())){
            menu.saveProductContent(vertx);
        }else if (String.format("/menu/update-product/%s", context.request().getParam("id")).equalsIgnoreCase(context.request().path())){
            String id = context.request().getParam("id");
            menu.updateProductForm(templateEngine,vertx,id);
        }else if("/menu/update-product".equalsIgnoreCase(context.request().path())){
            menu.saveUpdateProduct(vertx);
        }
    }

}
