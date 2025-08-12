package com.example.starter;

import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;

public class Launcher {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Deploying " + cores + " instances");

        vertx.deployVerticle(
            MainVerticle::new,
            new DeploymentOptions().setInstances(cores) // 1 per CPU core
        );
    }
}
