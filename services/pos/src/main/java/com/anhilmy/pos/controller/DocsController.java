package com.anhilmy.pos.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static spark.Spark.get;

/** Serves the OpenAPI specification and an interactive Swagger UI page. */
public class DocsController {

    private static final String SWAGGER_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <title>POS API Documentation</title>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script>
                window.onload = () => {
                  window.ui = SwaggerUIBundle({
                    url: '/openapi.yaml',
                    dom_id: '#swagger-ui',
                    presets: [SwaggerUIBundle.presets.apis],
                  });
                };
              </script>
            </body>
            </html>
            """;

    public static void registerRoutes() {
        get("/openapi.yaml", (req, res) -> {
            res.type("application/yaml");
            return readSpec();
        });

        get("/docs", (req, res) -> {
            res.type("text/html");
            return SWAGGER_HTML;
        });

        get("/docs/", (req, res) -> {
            res.redirect("/docs");
            return "";
        });
    }

    private static String readSpec() {
        try (InputStream in = DocsController.class.getResourceAsStream("/openapi.yaml")) {
            if (in == null) {
                return "openapi: 3.0.3\ninfo:\n  title: POS API\n  version: unknown\n";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "openapi: 3.0.3\ninfo:\n  title: POS API\n  version: unknown\n";
        }
    }
}
