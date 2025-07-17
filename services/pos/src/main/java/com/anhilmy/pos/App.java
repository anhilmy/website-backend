// App.java
package com.anhilmy.pos;

import static spark.Spark.*;
import com.anhilmy.pos.controller.UserController;

public class App {
    public static void main(String[] args) {
        port(4567);
        UserController.registerRoutes();
    }
}
