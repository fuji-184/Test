package com.example.starter.model;

import com.julienviet.jsonsergen.Backend;
import com.julienviet.jsonsergen.JsonSerGen;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.buffer.Buffer;

  @DataObject
  @JsonSerGen(backends = Backend.DSL_JSON)
  public final class User {
    private final String name;

    public User(String name){
      this.name = name;
    }

public String getName() { return name; }

    public Buffer toJson(){
      return UserJsonSerializer.toJsonBuffer(this);
    }

 public static Buffer toJson(User[] users) {
    return UserJsonSerializer.toJsonBuffer(users);
  }
  }


